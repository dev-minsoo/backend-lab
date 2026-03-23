package com.lab.onlineqna.repository

import com.lab.onlineqna.domain.Report
import org.springframework.data.jpa.repository.JpaRepository

interface ReportRepository : JpaRepository<Report, Long>
