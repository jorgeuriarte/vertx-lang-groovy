package io.vertx.lang.groovy;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.lang.groovy.support.LifeCycleVerticleClass;
import io.vertx.test.core.AsyncTestBase;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class DeploymentTest extends AsyncTestBase {

  public static final CyclicBarrier started = new CyclicBarrier(2);
  public static final CyclicBarrier stopped = new CyclicBarrier(2);

  @Before
  public void before() {
    started.reset();
  }

  @Test
  public void testDeployVerticleGroovyClass() throws Exception {
    assertDeploy((vertx, onDeploy) ->
        vertx.deployVerticle(
            "groovy:verticles/compile/LifeCycleVerticleClass.groovy",
            DeploymentOptions.options(),
            onDeploy));
  }

  @Test
  public void testDeployVerticleInstance() throws Exception {
    assertDeploy((vertx, onDeploy) ->
        vertx.deployVerticleInstance(
            new LifeCycleVerticleClass().asJavaVerticle(),
            DeploymentOptions.options(),
            onDeploy));
  }

  @Test
  public void testDeployVerticleGroovyScript() throws Exception {
    assertDeploy((vertx, onDeploy) ->
        vertx.deployVerticle(
            "groovy:verticles/compile/LifeCycleVerticleScript.groovy",
            DeploymentOptions.options(),
            onDeploy));
  }

  @Test
  public void testDeployVerticleGroovyScriptNoStop() throws Exception {
    Vertx vertx = Vertx.vertx();
    try {
      BlockingQueue<AsyncResult<String>> deployed = new ArrayBlockingQueue<>(1);
      vertx.deployVerticle("groovy:verticles/compile/NoStopVerticleScript.groovy", DeploymentOptions.options(), deployed::add);
      AsyncResult<String> deployedResult = deployed.poll(10, TimeUnit.SECONDS);
      assertTrue(deployedResult.succeeded());
      BlockingQueue<AsyncResult<Void>> undeployed = new ArrayBlockingQueue<>(1);
      vertx.undeployVerticle(deployedResult.result(), undeployed::add);
      AsyncResult<?> undeployedResult = undeployed.poll(10, TimeUnit.SECONDS);
      assertTrue(undeployedResult.succeeded());
    } finally {
      vertx.close();
    }
  }

  @FunctionalInterface
  static interface Deployer {
    void deploy(Vertx vertx, Handler<AsyncResult<String>> onDeploy);
  }

  private void assertDeploy(Deployer deployer) throws Exception {
    Vertx vertx = Vertx.vertx();
    try {
      BlockingQueue<AsyncResult<String>> deployed = new ArrayBlockingQueue<>(1);
      deployer.deploy(vertx, deployed::add);
      started.await(10, TimeUnit.SECONDS);
      AsyncResult<String> deployment = deployed.poll(10, TimeUnit.SECONDS);
      assertTrue(deployment.succeeded());
      vertx.undeployVerticle(deployment.result(), null);
      stopped.await(10, TimeUnit.SECONDS);
    } finally {
      vertx.close();
    }
  }
}