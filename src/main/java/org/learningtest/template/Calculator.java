package org.learningtest.template;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Calculator {
    public Integer calcSum(String filePath) throws IOException {
    LineCallback sumCallback = new LineCallback() {
        @Override
        public Integer doSomethingWithLine(String line, Integer value) {
            return value + Integer.valueOf(line);
        }
    };
        return lineReadTemplate(filePath, sumCallback, 0);
    }

    public Integer calMultiply(String filPath) throws IOException {
       LineCallback multiplyCallback = new LineCallback() {
           @Override
           public Integer doSomethingWithLine(String line, Integer value) {
               return value * Integer.valueOf(line);
           }
       };

        return lineReadTemplate(filPath, multiplyCallback, 1);
    }

    public Integer fileReadTemplate(String filePath, BufferedReaderCallback callback) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filePath));
            int ret = callback.doSomethingWithReader(br); // 템플릿에서 만든 컨텍스트 정보 콜백에 전달
            return ret;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw e;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    System.out.println();
                }
            }
        }
    }

    public Integer lineReadTemplate(String filePath, LineCallback callback, int initVal) throws IOException {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(filePath));
            Integer res = initVal;
            String line = null;
            while ((line = br.readLine()) != null) {
                res = callback.doSomethingWithLine(line, res);
            }
            return res;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw e;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    System.out.println();
                }
            }
        }
    }
}
