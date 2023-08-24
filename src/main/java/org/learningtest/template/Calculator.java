package org.learningtest.template;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Calculator {
    public Integer calcSum(String filePath) throws IOException {
        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(filePath)); // 한 줄씩 읽기 편하게 BufferedReader
            Integer sum = 0;
            String line = null;
            while ((line = br.readLine()) != null) {
                sum += Integer.valueOf(line);
            }
            return sum;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            throw  e;
        } finally {
            if(br != null){ // BufferedReader 오브젝트가 생성되기 전에 예외가 발생할 수도 있으므로 반드시 null 체크
                try{ br.close();}
                catch (IOException e){
                    System.out.println(e.getMessage());
                }
            }
        }
    }
}
