package com.lab.onlineqna.config

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class UsePrimaryDataSource

object DataSourceRoutingContextHolder {
    private val forceWriteDepth = ThreadLocal.withInitial { 0 }

    fun pushForceWrite() {
        forceWriteDepth.set(forceWriteDepth.get() + 1)
    }

    fun popForceWrite() {
        val next = forceWriteDepth.get() - 1
        if (next <= 0) {
            forceWriteDepth.remove()
        } else {
            forceWriteDepth.set(next)
        }
    }

    fun isForceWrite(): Boolean = forceWriteDepth.get() > 0
}

object DataSourceRoutingDecider {
    fun determine(isReadOnlyTransaction: Boolean, isForceWrite: Boolean): DataSourceType =
        when {
            isForceWrite -> DataSourceType.WRITE
            isReadOnlyTransaction -> DataSourceType.READ
            else -> DataSourceType.WRITE
        }
}

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class UsePrimaryDataSourceAspect {

    @Around("@within(com.lab.onlineqna.config.UsePrimaryDataSource) || @annotation(com.lab.onlineqna.config.UsePrimaryDataSource)")
    fun forcePrimary(joinPoint: ProceedingJoinPoint): Any? {
        DataSourceRoutingContextHolder.pushForceWrite()
        return try {
            joinPoint.proceed()
        } finally {
            DataSourceRoutingContextHolder.popForceWrite()
        }
    }
}
