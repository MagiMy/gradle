/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.model.internal.core;

import org.gradle.api.Action;
import org.gradle.internal.Actions;
import org.gradle.internal.BiAction;
import org.gradle.internal.util.BiFunction;
import org.gradle.model.ModelMap;
import org.gradle.model.RuleSource;
import org.gradle.model.internal.core.rule.describe.ModelRuleDescriptor;
import org.gradle.model.internal.core.rule.describe.NestedModelRuleDescriptor;
import org.gradle.model.internal.core.rule.describe.SimpleModelRuleDescriptor;
import org.gradle.model.internal.type.ModelType;

import java.util.Collection;
import java.util.List;

import static org.gradle.internal.Cast.uncheckedCast;

public class DefaultModelMap<T> extends AbstractModelMap<T> {
    private final BiFunction<? extends ModelCreators.Builder, ? super MutableModelNode, ? super ModelReference<? extends T>> creatorFunction;

    public DefaultModelMap(ModelType<T> elementType, ModelRuleDescriptor sourceDescriptor, MutableModelNode modelNode,
                           BiFunction<? extends ModelCreators.Builder, ? super MutableModelNode, ? super ModelReference<? extends T>> creatorFunction) {
        super(elementType, modelNode, sourceDescriptor);
        this.creatorFunction = creatorFunction;
    }

    @Override
    public String toString() {
        return ModelMap.class.getSimpleName() + '<' + elementType.getSimpleName() + "> '" + modelNode.getPath() + "'";
    }

    @Override
    public boolean containsValue(Object item) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public void create(final String name) {
        doCreate(name, elementType);
    }

    @Override
    public void create(String name, Action<? super T> configAction) {
        doCreate(name, elementType, configAction);
    }

    @Override
    public <S extends T> void create(final String name, final Class<S> type) {
        doCreate(name, ModelType.of(type));
    }

    @Override
    public <S extends T> void create(final String name, final Class<S> type, final Action<? super S> configAction) {
        doCreate(name, ModelType.of(type), configAction);
    }

    private <S extends T> void doCreate(final String name, final ModelType<S> type) {
        doCreate(name, type, Actions.doNothing());
    }

    private <S extends T> void doCreate(final String name, final ModelType<S> type, final Action<? super S> initAction) {
        ModelRuleDescriptor descriptor = NestedModelRuleDescriptor.append(sourceDescriptor, "create(%s)", name);

        ModelCreators.Builder creatorBuilder = creatorFunction.apply(modelNode, ModelReference.of(modelNode.getPath().child(name), type));

        ModelCreator creator = creatorBuilder
            .withProjection(new UnmanagedModelProjection<S>(type, true, true))
            .descriptor(descriptor)
            .build();

        modelNode.addLink(creator);

        modelNode.applyToLink(ModelActionRole.Initialize, new ActionBackedModelAction<S>(ModelReference.of(creator.getPath(), type), descriptor, new Action<S>() {
            @Override
            public void execute(S s) {
                initAction.execute(s);
            }
        }));

        onCreate(name, type);
    }

    protected <S extends T> void onCreate(String name, ModelType<S> type) {

    }

    @Override
    public void named(final String name, Action<? super T> configAction) {
        ModelRuleDescriptor descriptor = NestedModelRuleDescriptor.append(sourceDescriptor, "named(%s)", name);
        ModelReference<T> subject = ModelReference.of(modelNode.getPath().child(name), elementType);
        modelNode.applyToLink(ModelActionRole.Mutate, new ActionBackedModelAction<T>(subject, descriptor, configAction));
    }

    @Override
    public void named(String name, Class<? extends RuleSource> ruleSource) {
        modelNode.applyToLink(name, ruleSource);
    }

    @Override
    public void all(final Action<? super T> configAction) {
        ModelRuleDescriptor descriptor = NestedModelRuleDescriptor.append(sourceDescriptor, "all()");
        ModelReference<T> subject = ModelReference.of(elementType);
        modelNode.applyToAllLinks(ModelActionRole.Mutate, new ActionBackedModelAction<T>(subject, descriptor, configAction));
    }

    @Override
    public <S> void withType(Class<S> type, Action<? super S> configAction) {
        ModelRuleDescriptor descriptor = NestedModelRuleDescriptor.append(sourceDescriptor, "withType()");
        ModelReference<S> subject = ModelReference.of(type);
        modelNode.applyToAllLinks(ModelActionRole.Mutate, new ActionBackedModelAction<S>(subject, descriptor, configAction));
    }

    @Override
    public <S> void withType(Class<S> type, Class<? extends RuleSource> rules) {
        modelNode.applyToLinks(type, rules);
    }

    @Override
    public void beforeEach(Action<? super T> configAction) {
        doBeforeEach(elementType, configAction);
    }

    @Override
    public <S> void beforeEach(Class<S> type, Action<? super S> configAction) {
        doBeforeEach(ModelType.of(type), configAction);
    }

    private <S> void doBeforeEach(ModelType<S> type, Action<? super S> configAction) {
        ModelRuleDescriptor descriptor = NestedModelRuleDescriptor.append(sourceDescriptor, "beforeEach()");
        ModelReference<S> subject = ModelReference.of(type);
        modelNode.applyToAllLinks(ModelActionRole.Defaults, new ActionBackedModelAction<S>(subject, descriptor, configAction));
    }

    @Override
    public void afterEach(Action<? super T> configAction) {
        doFinalizeAll(elementType, configAction);
    }

    @Override
    public <S> void afterEach(Class<S> type, Action<? super S> configAction) {
        doFinalizeAll(ModelType.of(type), configAction);
    }

    private <S> void doFinalizeAll(ModelType<S> type, Action<? super S> configAction) {
        ModelRuleDescriptor descriptor = NestedModelRuleDescriptor.append(sourceDescriptor, "afterEach()");
        ModelReference<S> subject = ModelReference.of(type);
        modelNode.applyToAllLinks(ModelActionRole.Finalize, new ActionBackedModelAction<S>(subject, descriptor, configAction));
    }

    public static <I> ModelType<ModelMap<I>> modelMapTypeOf(Class<I> type) {
        return modelMapTypeOf(ModelType.of(type));
    }

    public static <I> ModelType<ModelMap<I>> modelMapTypeOf(ModelType<I> type) {
        return new ModelType.Builder<ModelMap<I>>() {
        }.where(
            new ModelType.Parameter<I>() {
            }, type
        ).build();
    }

    public static <T> BiFunction<ModelCreators.Builder, MutableModelNode, ModelReference<? extends T>> createUsingParentNode(final ModelType<T> baseItemModelType) {
        return new BiFunction<ModelCreators.Builder, MutableModelNode, ModelReference<? extends T>>() {
            @Override
            public ModelCreators.Builder apply(final MutableModelNode parent, ModelReference<? extends T> reference) {
                final ModelPath path = reference.getPath();
                final ModelType<? extends T> modelType = reference.getType();
                return ModelCreators
                        .of(ModelReference.of(path, modelType), new BiAction<MutableModelNode, List<ModelView<?>>>() {
                            @Override
                            public void execute(MutableModelNode modelNode, List<ModelView<?>> modelViews) {
                                doExecute(modelNode, modelType);
                            }

                            public <S extends T> void doExecute(MutableModelNode modelNode, ModelType<S> subType) {
                                NamedEntityInstantiator<T> instantiator = parent.getPrivateData(instantiatorTypeOf(baseItemModelType));
                                S item = instantiator.create(path.getName(), subType.getConcreteClass());
                                modelNode.setPrivateData(subType, item);
                            }
                        });
            }
        };
    }

    public static <T> BiFunction<ModelCreators.Builder, MutableModelNode, ModelReference<? extends T>> createAndStoreVia(final ModelReference<? extends NamedEntityInstantiator<? super T>> instantiatorReference, final ModelReference<? extends Collection<? super T>> storeReference) {
        return new BiFunction<ModelCreators.Builder, MutableModelNode, ModelReference<? extends T>>() {
            @Override
            public ModelCreators.Builder apply(MutableModelNode parent, ModelReference<? extends T> reference) {
                final ModelPath path = reference.getPath();
                final ModelType<? extends T> modelType = reference.getType();
                return ModelCreators
                        .of(ModelReference.of(path, modelType), new BiAction<MutableModelNode, List<ModelView<?>>>() {
                            @Override
                            public void execute(MutableModelNode modelNode, List<ModelView<?>> modelViews) {
                                doExecute(modelNode, modelType, modelViews);
                            }

                            public <S extends T> void doExecute(MutableModelNode modelNode, ModelType<S> subType, List<ModelView<?>> modelViews) {
                                NamedEntityInstantiator<? super T> instantiator = ModelViews.getInstance(modelViews.get(0), instantiatorReference);
                                S item = instantiator.create(path.getName(), subType.getConcreteClass());
                                modelNode.setPrivateData(subType, item);
                                modelNode.applyToSelf(ModelActionRole.Initialize, BiActionBackedModelAction.single(ModelReference.of(path, subType), new SimpleModelRuleDescriptor("DefaultModelMap.createAndStoreVia() - " + path), storeReference, new BiAction<S, Collection<? super T>>() {
                                    @Override
                                    public void execute(S s, Collection<? super T> objects) {
                                        objects.add(s);
                                    }
                                }));
                            }
                        })
                        .inputs(instantiatorReference);

            }
        };
    }

    public static <T> BiFunction<ModelCreators.Builder, MutableModelNode, ModelReference<? extends T>> createVia(final ModelReference<? extends NamedEntityInstantiator<? super T>> instantiatorReference) {
        return new BiFunction<ModelCreators.Builder, MutableModelNode, ModelReference<? extends T>>() {
            @Override
            public ModelCreators.Builder apply(MutableModelNode parent, ModelReference<? extends T> reference) {
                final ModelPath path = reference.getPath();
                final ModelType<? extends T> modelType = reference.getType();
                return ModelCreators
                        .of(ModelReference.of(path, modelType), new BiAction<MutableModelNode, List<ModelView<?>>>() {
                            @Override
                            public void execute(MutableModelNode modelNode, List<ModelView<?>> modelViews) {
                                doExecute(modelNode, modelType, modelViews);
                            }

                            public <S extends T> void doExecute(MutableModelNode modelNode, ModelType<S> subType, List<ModelView<?>> modelViews) {
                                NamedEntityInstantiator<? super T> instantiator = ModelViews.getInstance(modelViews.get(0), instantiatorReference);
                                S item = instantiator.create(path.getName(), subType.getConcreteClass());
                                modelNode.setPrivateData(subType, item);
                            }
                        })
                        .inputs(instantiatorReference);

            }
        };
    }

    public static <I> ModelType<NamedEntityInstantiator<I>> instantiatorTypeOf(ModelType<I> type) {
        return new ModelType.Builder<NamedEntityInstantiator<I>>() {
        }.where(
                new ModelType.Parameter<I>() {
                }, type
        ).build();
    }

    public static <I> ModelType<NamedEntityInstantiator<I>> instantiatorTypeOf(Class<I> type) {
        return instantiatorTypeOf(ModelType.of(type));
    }

    @Override
    public <S> ModelMap<S> withType(Class<S> type) {
        if (type.equals(elementType.getConcreteClass())) {
            return uncheckedCast(this);
        }

        if (elementType.getConcreteClass().isAssignableFrom(type)) {
            Class<? extends T> castType = uncheckedCast(type);
            ModelMap<? extends T> subType = toSubType(castType);
            return uncheckedCast(subType);
        }

        return new DefaultModelMap<S>(ModelType.of(type), sourceDescriptor, modelNode, new BiFunction<ModelCreators.Builder, MutableModelNode, ModelReference<? extends S>>() {
            @Override
            public ModelCreators.Builder apply(MutableModelNode parent, ModelReference<? extends S> reference) {
                throw new IllegalArgumentException(String.format("Cannot create an item of type %s as this is not a subtype of %s.", reference.getType(), elementType.toString()));
            }
        });
    }

    public <S extends T> ModelMap<S> toSubType(Class<S> type) {
        return new DefaultModelMap<S>(ModelType.of(type), sourceDescriptor, modelNode, creatorFunction);
    }
}
