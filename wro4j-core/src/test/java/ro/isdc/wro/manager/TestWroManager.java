/*
 * Copyright (c) 2008. All rights reserved.
 */
package ro.isdc.wro.manager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ro.isdc.wro.AbstractWroTest;
import ro.isdc.wro.config.Context;
import ro.isdc.wro.config.jmx.WroConfiguration;
import ro.isdc.wro.http.DelegatingServletOutputStream;
import ro.isdc.wro.manager.factory.NoProcessorsWroManagerFactory;
import ro.isdc.wro.manager.factory.ServletContextAwareWroManagerFactory;
import ro.isdc.wro.model.factory.XmlModelFactory;
import ro.isdc.wro.test.util.WroTestUtils;


/**
 * TestWroManager.java.
 *
 * @author Alex Objelean
 * @created Created on Nov 3, 2008
 */
public class TestWroManager extends AbstractWroTest {
  private WroManager manager;


  @Before
  public void setUp() {

    final Context context = Context.webContext(Mockito.mock(HttpServletRequest.class),
      Mockito.mock(HttpServletResponse.class, Mockito.RETURNS_DEEP_STUBS), Mockito.mock(FilterConfig.class));
    Context.set(context, newConfigWithUpdatePeriodValue(0));
    final WroManagerFactory factory = new ServletContextAwareWroManagerFactory();
    manager = factory.getInstance();
  }


  @Test
  public void testNoProcessorWroManagerFactory() throws IOException {
    final WroManagerFactory factory = new NoProcessorsWroManagerFactory();
    manager = factory.getInstance();
    manager.setModelFactory(getValidModelFactory());
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    final HttpServletResponse response = Context.get().getResponse();

    final ByteArrayOutputStream out = new ByteArrayOutputStream();
    Mockito.when(response.getOutputStream()).thenReturn(new DelegatingServletOutputStream(out));
    Mockito.when(request.getRequestURI()).thenReturn("/app/g1.css");
    manager.process(request, response);
    //compare written bytes to output stream with the content from specified css.
    WroTestUtils.compare(getInputStream("classpath:ro/isdc/wro/manager/noProcessorsResult.css"), new ByteArrayInputStream(out.toByteArray()));
  }


  @Test
  public void processValidModel() throws IOException {
    manager.setModelFactory(getValidModelFactory());
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    Mockito.when(request.getRequestURI()).thenReturn("/app/g1.css");
    manager.process(request, Context.get().getResponse());
  }


  /**
   * @return a {@link XmlModelFactory} pointing to a valid config resource.
   */
  private XmlModelFactory getValidModelFactory() {
    return new XmlModelFactory() {
      @Override
      protected InputStream getConfigResourceAsStream() {
        return getResourceAsStream(TestWroManager.class.getPackage().getName().replace(".", "/") + "/wro.xml");
      }
    };
  }


  /**
   * Test how manager behaves when the update period value is greater than zero and the scheduler starts.
   *
   * @throws Exception
   */
  @Test
  public void testManagerWhenSchedulerIsStarted() throws Exception {
    manager.setModelFactory(getValidModelFactory());
    newConfigWithUpdatePeriodValue(1);
    final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
    final HttpServletResponse response = Context.get().getResponse();
    Mockito.when(request.getRequestURI()).thenReturn("/app/g1.css");
    manager.process(request, response);
    // allow thread to do its job
    Thread.sleep(500);
  }


  /**
   * Initialize {@link WroConfiguration} object with cacheUpdatePeriod & modelUpdatePeriod equal with provided argument.
   */
  private WroConfiguration newConfigWithUpdatePeriodValue(final long periodValue) {
    final WroConfiguration config = new WroConfiguration();
    config.setCacheUpdatePeriod(periodValue);
    config.setModelUpdatePeriod(periodValue);
    return config;
  }


  @After
  public void tearDown() {
    manager.destroy();
    Context.unset();
  }
}
