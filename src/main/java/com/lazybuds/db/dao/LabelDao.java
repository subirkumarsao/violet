package com.lazybuds.db.dao;

import java.util.List;
import java.util.Optional;

import javax.persistence.Query;

import org.springframework.stereotype.Component;

import com.lazybuds.db.entity.Label;

@Component
public class LabelDao extends AbstractDao<Label> {

	public LabelDao() {
		super(Label.class);
	}

	public Optional<Label> getByName(String name) {
		Query query = sessionFactory.getCurrentSession().createQuery("from Label where name=:name");
		query.setParameter("name", name);
		
		@SuppressWarnings("unchecked")
		List<Label> results = query.getResultList();
		if(results.size()!=1) {
			return Optional.empty();
		}
		return Optional.of(results.get(0));
	}
}
