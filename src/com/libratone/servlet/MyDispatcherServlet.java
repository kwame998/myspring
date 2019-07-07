package com.libratone.servlet;

import com.libratone.annotation.MyController;
import com.libratone.annotation.MyRequestMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.swing.text.html.parser.Entity;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class MyDispatcherServlet extends HttpServlet {

  private Properties properties = new Properties();
  private List<String> classNames = new ArrayList<>();
  private Map<String, Object> ioc = new HashMap<>();
  private Map<String, Method> handlerMapping = new HashMap<>();
  private Map<String, Object> controllerMap = new HashMap<>();


  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    this.doPost(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    try {
      doDispatch(req, resp);
    } catch (Exception e) {
      resp.getWriter().write("500 !! server Exception");
    }
  }

  private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    if (handlerMapping.isEmpty()) {
      return;
    }
    String url = req.getRequestURI();
    String contextPath = req.getContextPath();

    url = url.replace(contextPath, "").replaceAll("/+", "/");
    if (!this.handlerMapping.containsKey(url)) {
      resp.getWriter().write("404 not found");
      return;
    }

    Method method = this.handlerMapping.get(url);

    // 获取参数列表

    Class<?>[] parameterTypes = method.getParameterTypes();

    //获取请求的参数

    Map<String, String[]> parameteMap = req.getParameterMap();

    //save param

    Object[] paramValues = new Object[parameterTypes.length];

    for (int i = 0; i < parameterTypes.length; i++) {
      String requestParam = parameterTypes[i].getSimpleName();
      //参数类型已明确，这边强转类型

      if (requestParam.equals("HttpServletRequest")) {
        paramValues[i] = req;
        continue;
      }

      if (requestParam.equals("HttpServletResponse")) {
        paramValues[i] = resp;
        continue;
      }

      if (requestParam.equals("String")) {
        for (Map.Entry<String, String[]> param : parameteMap.entrySet()) {
          String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
          paramValues[i] = value;
        }
      }

    }
    try {
      method.invoke(this.controllerMap.get(url), paramValues);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    //1 加载配置文件
    doLoadConfig(config.getInitParameter("contextConfigLocation"));

    //2 初使化所有相关类，扫描用户设定的包下面所有包
    doScanner(properties.getProperty("scanPackage"));

    //3  拿到扫描到的类
    doInstance();

    //4初使化所有的handlerMapping (将URL和method对上）
    initHandlerMapping();
  }

  private void initHandlerMapping() {
    if (ioc.isEmpty()) {
      return;
    }
    try {
      for (Map.Entry<String, Object> entry : ioc.entrySet()) {
        Class<? extends Object> clazz = entry.getValue().getClass();
        if (!clazz.isAnnotationPresent(MyController.class)) {
          continue;
        }

        //拼url时 是 controller头的url 拼上方法url
        String baseUrl = "";
        if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
          MyRequestMapping annotation = clazz.getAnnotation(MyRequestMapping.class);
          baseUrl = annotation.value();
        }
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
          if (!method.isAnnotationPresent(MyRequestMapping.class)) {
            continue;
          }
          MyRequestMapping anntation = method.getAnnotation(MyRequestMapping.class);
          String url = anntation.value();
          url = (baseUrl + "/" + url).replaceAll("/+", "/");
          handlerMapping.put(url, method);
          controllerMap.put(url, clazz.newInstance());
          System.out.println(url+ ","+ method);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void doInstance() {
    if (classNames.isEmpty()) {
      return;
    }
    for (String clasName : classNames) {
      try {
        Class<?> clazz = Class.forName(clasName);
        if (clazz.isAnnotationPresent(MyController.class)) {
          ioc.put(toLowerFirstWord(clazz.getSimpleName()), clazz.newInstance());
          System.out.println(String.format("add ioc : %s",toLowerFirstWord(clazz.getSimpleName())));
        } else {
          continue;
        }
      } catch (Exception e) {
        e.printStackTrace();
        continue;
      }
    }
  }

  private void doScanner(String pacakgeName) {
    URL url = this.getClass().getClassLoader().getResource("/" + pacakgeName.replaceAll("\\.", "/"));
    File dir = new File(url.getFile());
    for (File file : dir.listFiles()) {
      if (file.isDirectory()) {
        doScanner(pacakgeName + "." + file.getName());
      } else {
        String className = pacakgeName + "." + file.getName().replace(".class", "");
        System.out.println(String.format("add classname: %s", className));
        classNames.add(className);
      }
    }

  }

  private void doLoadConfig(String location) {
    //把web.xml中的contextConfigLocation对应value值的文件加载到流里面
    InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(location);

    try {
      properties.load(resourceAsStream);
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (null != resourceAsStream) {
        try {
          resourceAsStream.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

  }

  /**
   * 把字符串的首字母小写
   *
   * @param name
   * @return
   */
  private String toLowerFirstWord(String name) {
    char[] charArray = name.toCharArray();
    charArray[0] += 32;
    return String.valueOf(charArray);
  }

}
