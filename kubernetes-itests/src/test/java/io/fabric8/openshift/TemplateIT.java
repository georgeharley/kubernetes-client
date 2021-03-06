/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.openshift;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.openshift.api.model.Parameter;
import io.fabric8.openshift.api.model.Template;
import io.fabric8.openshift.api.model.TemplateBuilder;
import io.fabric8.openshift.api.model.TemplateList;
import io.fabric8.openshift.client.OpenShiftClient;
import org.arquillian.cube.kubernetes.api.Session;
import org.arquillian.cube.openshift.impl.requirement.RequiresOpenshift;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;

import static io.fabric8.kubernetes.client.utils.ReplaceValueStream.replaceValues;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(ArquillianConditionalRunner.class)
@RequiresOpenshift
public class TemplateIT {
  @ArquillianResource
  OpenShiftClient client;

  @ArquillianResource
  Session session;

  private Template template1;

  private String currentNamespace;

  @Before
  public void init() {
    currentNamespace = session.getNamespace();
    Service aService = new ServiceBuilder()
      .withNewMetadata().withName("bar").endMetadata()
      .withNewSpec()
      .addNewPort()
      .withPort(80).endPort()
      .addToSelector("cheese", "edam")
      .withType("ExternalName")
      .endSpec()
      .build();

    template1 = new TemplateBuilder()
      .withNewMetadata().withName("foo").endMetadata()
      .addToObjects(aService)
      .build();

    client.templates().inNamespace(currentNamespace).create(template1);
  }

  @Test
  public void load() {
    Template template = client.templates().inNamespace(currentNamespace).load(replaceValues(
      getClass().getResourceAsStream("/test-template.yml"), Collections.singletonMap("REDIS_PASSWORD", "secret"))
    ).get();
    assertThat(template).isNotNull();
    assertEquals(1, template.getObjects().size());
  }

  @Test
  public void get() {
    template1 = client.templates().inNamespace(currentNamespace).withName("foo").get();
    assertNotNull(template1);
  }

  @Test
  public void list() {
    TemplateList aList = client.templates().inNamespace(currentNamespace).list();
    assertThat(aList).isNotNull();
    assertEquals(1, aList.getItems().size());
  }

  @Test
  public void delete() {
    boolean bDeleted = client.templates().inNamespace(currentNamespace).withName("foo").delete();
    assertTrue(bDeleted);
  }

  @After
  public void cleanup() {
    client.templates().inNamespace(currentNamespace).delete();
  }
}
