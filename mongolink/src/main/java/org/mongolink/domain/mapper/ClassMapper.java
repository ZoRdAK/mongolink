/*
 * MongoLink, Object Document Mapper for Java and MongoDB
 *
 * Copyright (c) 2012, Arpinum or third-party contributors as
 * indicated by the @author tags
 *
 * MongoLink is free software: you can redistribute it and/or modify
 * it under the terms of the Lesser GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MongoLink is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Lesser GNU General Public License for more details.
 *
 * You should have received a copy of the Lesser GNU General Public License
 * along with MongoLink.  If not, see <http://www.gnu.org/licenses/>. 
 *
 */

package org.mongolink.domain.mapper;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import net.sf.cglib.core.ReflectUtils;
import org.mongolink.domain.converter.Converter;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public abstract class ClassMapper<T> extends Converter implements Mapper {

    public ClassMapper(Class<T> persistentType) {
        this.persistentType = persistentType;
    }

    public Class<T> getPersistentType() {
        return persistentType;
    }

    void addCollection(CollectionMapper collection) {
        collection.setMapper(this);
        addMapper(collection);
    }

    public void addProperty(PropertyMapper property) {
        property.setMapper(this);
        addMapper(property);
    }

    void addMap(MapMapper mapMapper) {
        addMapper(mapMapper);
    }

    <U> void addSubclass(SubclassMapper<U> mapper) {
        subclasses.put(mapper.discriminator(), mapper);
    }

    protected void addMapper(Mapper property) {
        mappers.add(property);
    }

    @Override
    public Object fromDbValue(Object value) {
        return toInstance((DBObject) value);
    }

    public T toInstance(DBObject from) {
        T instance = makeInstance(from);
        populate(instance, from);
        return instance;
    }

    protected T makeInstance(final DBObject from) {
        if(from == null) {
            return null;
        }
        String discriminator = SubclassMapper.discriminatorValue(from);
        if (subclasses.get(discriminator) != null) {
            return (T) subclasses.get(discriminator).toInstance(from);
        }
        return (T) ReflectUtils.newInstance(persistentType);
    }

    @Override
    public void populate(Object instance, DBObject from) {
        for (Mapper mapper : mappers) {
            mapper.populate(instance, from);
        }
    }

    @Override
    public Object toDbValue(Object value) {
        return toDBObject(value);
    }

    public DBObject toDBObject(Object element) {
        DBObject object = createDbObject(element);
        save(element, object);
        return object;
    }

    private DBObject createDbObject(Object element) {
        if (isSubclass(element)) {
            return subclassMapperFor(element).toDBObject(element);
        }
        return new BasicDBObject();
    }

    private boolean isSubclass(Object element) {
        return subclassMapperFor(element) != null;
    }

    private SubclassMapper<?> subclassMapperFor(Object element) {
        Class<?> type = element.getClass();
        return getSubclass(type);
    }

    protected  <U> SubclassMapper<U> getSubclass(Class<U> type) {
        for (SubclassMapper<?> subclassMapper : subclasses.values()) {
            if (subclassMapper.getPersistentType().isAssignableFrom(type)) {
                return (SubclassMapper<U>) subclassMapper;
            }
        }
        return null;
    }

    @Override
    public void save(Object instance, DBObject into) {
        for (Mapper mapper : mappers) {
            mapper.save(instance, into);
        }
    }

    public MapperContext getContext() {
        return context;
    }

    void setContext(MapperContext context) {
        this.context = context;
    }

    public boolean canMap(Class<?> aClass) {
        return persistentType.isAssignableFrom(aClass);
    }

    public boolean isCapped() {
        return false;
    }

    protected final Class<T> persistentType;
    private final List<Mapper> mappers = Lists.newArrayList();
    private final Map<String, SubclassMapper<?>> subclasses = Maps.newHashMap();
    private MapperContext context;
}
