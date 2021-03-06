/**
*
* Copyright (c) 2017 ytk-mp4j https://github.com/yuantiku
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:

* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.

* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/

package com.fenbi.mp4j.check.checkbyte;

import com.fenbi.mp4j.check.ProcessCheck;
import com.fenbi.mp4j.comm.ProcessCommSlave;
import com.fenbi.mp4j.exception.Mp4jException;
import com.fenbi.mp4j.operand.Operands;
import com.fenbi.mp4j.operator.Operators;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xialong
 */
public class ProcessBroadcastCheck extends ProcessCheck {

    public ProcessBroadcastCheck(ProcessCommSlave slave, String serverHostName, int serverHostPort, int arrSize, int objSize, int runTime, boolean compress) {
        super(slave, serverHostName, serverHostPort, arrSize, objSize, runTime, compress);
    }

    @Override
    public void check() throws Mp4jException {
        int rank = slave.getRank();
        int slaveNum = slave.getSlaveNum();
        boolean success = true;
        long start;
        int rootRank = 0;
        byte []arr = new byte[arrSize];

        for (int rt = 1; rt <= runTime; rt++) {
            info("run time:" + rt + "...");
            // byte array
            info("begin to broadcast byte arr...");

            for (int i = 0; i < arrSize; i++) {
                if (rank == rootRank) {
                    arr[i] = 1;
                } else {
                    arr[i] = -1;
                }
            }


            start = System.currentTimeMillis();
            arr = slave.broadcastArray(arr, Operands.BYTE_OPERAND(compress), 0, arrSize, rootRank);
            info("broadcast byte arr takes:" + (System.currentTimeMillis() - start));

            for (int i = 0; i < arr.length; i++) {
                if (arr[i] != 1) {
                    info("broadcast byte array error:" + Arrays.toString(arr), false);
                    slave.close(1);
                }
            }
            info("broadcast byte arr success!");
            if (arrSize < 500) {
                info("broadcast result:" + Arrays.toString(arr));
            }

            // map
            info("begin to broadcast byte map...");
            Map<String, Byte> map = new HashMap<>();
            if (rank == rootRank) {
                for (int i = 0; i < objSize; i++) {
                    map.put(i + "", new Byte((byte)1));
                }
            }

            start = System.currentTimeMillis();
            Map<String, Byte> retMap = slave.broadcastMap(map, Operands.BYTE_OPERAND(compress), rootRank);
            info("broadcast byte map takes:" + (System.currentTimeMillis() - start));

            success = true;

            for (int i = 0; i < objSize; i++) {
                String key = i + "";
                Byte val = retMap.get(key);
                if (val == null || val.intValue() != 1) {
                    success = false;
                }

            }

            if (!success) {
                info("broadcast byte map error:" + retMap);
                slave.close(1);
            }

            if (objSize < 500) {
                info("broadcast byte map list:" + retMap.toString());
            }
            info("broadcast byte map success!");

            // single byte
            byte singleByte = slave.broadcast((byte)rank, Operands.BYTE_OPERAND(compress), rootRank);
            if (singleByte != rootRank) {
                info("broadcast single byte error:" + singleByte, false);
                slave.close(1);
            }
            info("broadcast single byte success!");
        }

    }
}
