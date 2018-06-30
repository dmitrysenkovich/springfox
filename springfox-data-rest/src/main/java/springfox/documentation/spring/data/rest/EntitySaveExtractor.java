/*
 *
 *  Copyright 2017-2019 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package springfox.documentation.spring.data.rest;

import com.fasterxml.classmate.TypeResolver;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.core.CrudMethods;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import springfox.documentation.RequestHandler;
import springfox.documentation.service.ResolvedMethodParameter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;



import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.springframework.web.bind.annotation.RequestMethod.*;
import static springfox.documentation.spring.data.rest.RequestExtractionUtils.*;

class EntitySaveExtractor implements EntityOperationsExtractor {
  @Override
  public List<RequestHandler> extract(EntityContext context) {
    final List<RequestHandler> handlers = new ArrayList<>();
    final PersistentEntity<?, ?> entity = context.entity();
    CrudMethods crudMethods = context.crudMethods();
    Object getSaveMethod = crudMethods.getSaveMethod();
    OptionalDeferencer<Method> converter = new OptionalDeferencer<>();
    if (crudMethods.hasSaveMethod()) {
      Method actualSaveMethod = converter.convert(getSaveMethod);
      HandlerMethod handler = new HandlerMethod(
          context.getRepositoryInstance(),
          actualSaveMethod);
      RepositoryMetadata resource = context.getRepositoryMetadata();
      ActionSpecification put = saveActionSpecification(
          entity,
          Stream.of(PUT, PATCH).collect(toSet()),
          String.format("%s%s/{id}",
              context.basePath(),
              context.resourcePath()),
          handler,
          context.getTypeResolver(),
          resource, Stream.of(
              new ResolvedMethodParameter(
                  0,
                  "id",
                  pathAnnotations("id", handler),
                  context.getTypeResolver().resolve(resource.getIdType())),
              new ResolvedMethodParameter(
                  0,
                  "body",
                  bodyAnnotations(handler),
                  context.getTypeResolver().resolve(resource.getDomainType()))).collect(toList()));
      handlers.add(new SpringDataRestRequestHandler(context, put));
      ActionSpecification post = saveActionSpecification(
          entity,
          singleton(POST),
          String.format("%s%s", context.basePath(), context.resourcePath()),
          handler,
          context.getTypeResolver(), resource, singletonList(
              new ResolvedMethodParameter(
                  0,
                  "body",
                  bodyAnnotations(handler),
                  context.getTypeResolver().resolve(resource.getDomainType()))));
      handlers.add(new SpringDataRestRequestHandler(context, post));
    }
    return handlers;
  }

  private ActionSpecification saveActionSpecification(
      PersistentEntity<?, ?> entity,
      Set<RequestMethod> methods,
      String path,
      HandlerMethod handler,
      TypeResolver typeResolver,
      RepositoryMetadata repository,
      List<ResolvedMethodParameter> parameters) {

    return new ActionSpecification(
        actionName(entity, handler.getMethod()),
        path,
        methods,
        new HashSet<MediaType>(),
        new HashSet<MediaType>(),
        handler,
        parameters,
        typeResolver.resolve(Resource.class, repository.getReturnedDomainClass(handler.getMethod())));
  }
}
