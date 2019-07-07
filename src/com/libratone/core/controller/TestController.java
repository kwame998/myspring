package com.libratone.core.controller;


import com.libratone.annotation.MyController;
import com.libratone.annotation.MyRequestMapping;
import com.libratone.annotation.MyRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@MyController
@MyRequestMapping("/test")
public class TestController {

  @MyRequestMapping("/doTest")
  public void test1(HttpServletRequest request, HttpServletResponse response,
                    @MyRequestParam("param") String param) {
    try {
      response.getWriter().write("do test Method success param: " + param)
      ;
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @MyRequestMapping("/doTest2")
  public void test2(HttpServletRequest request, HttpServletResponse response){
    try {
      response.getWriter().println("doTest2 method success!");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
