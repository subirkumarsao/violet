package com.lazybuds.db.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractDao<T>{
	
	private final Class<T> typeParameterClass;
	
	public AbstractDao(Class<T> typeParameterClass) {
		super();
		this.typeParameterClass = typeParameterClass;
	}

	@Autowired
	SessionFactory sessionFactory;
	
	public Session getSession(){
		return sessionFactory.getCurrentSession();
	}
	
	public Object save(T t) {
		return getSession().save(t);
	}
	
	public T getById(int id) {
		return sessionFactory.getCurrentSession().get(typeParameterClass, id);
	}
}