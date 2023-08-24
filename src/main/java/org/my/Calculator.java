package org.my;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

// 컨텍스트, 전략 분리하기..
// 컨텍스트 -> try/catch 반복 구조...
// 전략 인터페이스에 필요한 메소드는?
// 클라이언트는 어디가 되는가? -> test클래스..?
// 템플릿/콜백 패턴을 사용하기 위해 익명내부클래스는 어디에 어떻게 구현될 것인가?

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
