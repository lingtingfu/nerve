/**
 * MIT License
 * <p>
 Copyright (c) 2019-2020 nerve.network
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package nerve.network.converter.heterogeneouschain.eth.storage.impl;

import nerve.network.converter.heterogeneouschain.eth.constant.EthDBConstant;
import nerve.network.converter.heterogeneouschain.eth.model.EthUnconfirmedTxPo;
import nerve.network.converter.heterogeneouschain.eth.storage.EthUnconfirmedTxStorageService;
import nerve.network.converter.model.po.StringListPo;
import nerve.network.converter.utils.ConverterDBUtil;
import io.nuls.core.core.annotation.Component;
import io.nuls.core.rockdb.service.RocksDBService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static nerve.network.converter.heterogeneouschain.eth.context.EthContext.logger;
import static nerve.network.converter.utils.ConverterDBUtil.stringToBytes;

/**
 * @author: Chino
 * @date: 2020-02-20
 */
@Component
public class EthUnconfirmedTxStorageServiceImpl implements EthUnconfirmedTxStorageService {

    private final String baseArea = EthDBConstant.DB_ETH;
    private final String KEY_PREFIX = "UNCONFIRMED_TX-";
    private final byte[] UNCONFIRMED_TX_ALL_KEY = stringToBytes("UNCONFIRMED_TX-ALL");

    @Override
    public int save(EthUnconfirmedTxPo po) throws Exception {
        if (po == null) {
            return 0;
        }
        String ethTxHash = po.getTxHash();
        if(logger().isDebugEnabled()) {
            logger().debug("保存未确认交易[{}], 详情: {}", ethTxHash, po.toString());
        }
        boolean result = ConverterDBUtil.putModel(baseArea, stringToBytes(KEY_PREFIX + ethTxHash), po);
        if (result) {
            StringListPo setPo = ConverterDBUtil.getModel(baseArea, UNCONFIRMED_TX_ALL_KEY, StringListPo.class);
            if (setPo == null) {
                setPo = new StringListPo();
                List<String> list = new ArrayList<>();
                list.add(po.getTxHash());
                setPo.setCollection(list);
                result = ConverterDBUtil.putModel(baseArea, UNCONFIRMED_TX_ALL_KEY, setPo);
            } else {
                List<String> list = setPo.getCollection();
                Set<String> set = new HashSet<>(list);
                if (!set.contains(po.getTxHash())) {
                    list.add(po.getTxHash());
                    result = ConverterDBUtil.putModel(baseArea, UNCONFIRMED_TX_ALL_KEY, setPo);
                } else {
                    result = true;
                }
            }
        }
        return result ? 1 : 0;
    }

    @Override
    public EthUnconfirmedTxPo findByTxHash(String ethTxHash) {
        return ConverterDBUtil.getModel(baseArea, stringToBytes(KEY_PREFIX + ethTxHash), EthUnconfirmedTxPo.class);
    }

    @Override
    public void deleteByTxHash(String ethTxHash) throws Exception {
        RocksDBService.delete(baseArea, stringToBytes(KEY_PREFIX + ethTxHash));
        StringListPo setPo = ConverterDBUtil.getModel(baseArea, UNCONFIRMED_TX_ALL_KEY, StringListPo.class);
        setPo.getCollection().remove(ethTxHash);
        ConverterDBUtil.putModel(baseArea, UNCONFIRMED_TX_ALL_KEY, setPo);
    }

    @Override
    public List<EthUnconfirmedTxPo> findAll() {
        StringListPo setPo = ConverterDBUtil.getModel(baseArea, UNCONFIRMED_TX_ALL_KEY, StringListPo.class);
        if (setPo == null) {
            return null;
        }
        List<String> list = setPo.getCollection();
        List<EthUnconfirmedTxPo> resultList = new ArrayList<>();
        for (String txHash : list) {
            resultList.add(this.findByTxHash(txHash));
        }
        return resultList;
    }
}
