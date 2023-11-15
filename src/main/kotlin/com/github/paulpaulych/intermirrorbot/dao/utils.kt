package com.github.paulpaulych.intermirrorbot.dao

import jakarta.persistence.EntityManager
import jakarta.persistence.TypedQuery
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery


inline fun <reified T> EntityManager.selectWithCriteria(
    f: CriteriaBuilder.(CriteriaQuery<T>) -> CriteriaQuery<T>
): TypedQuery<T> {
    val cb = criteriaBuilder
    val query = cb.createQuery(T::class.java)
    cb.f(query)
    return createQuery(query)
}