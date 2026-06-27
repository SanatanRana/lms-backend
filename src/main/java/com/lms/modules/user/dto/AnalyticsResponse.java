package com.lms.modules.user.dto;

import lombok.Data;
import java.util.List;

@Data
public class AnalyticsResponse {
    private long totalStudents;
    private long totalTeachers;
    private long totalCourses;
    private long totalEnrollments;
    private double totalRevenue;
    private double monthlyRevenue;
    private double yearlyRevenue;
    private List<MonthlyRevenueStats> monthlyStats;
}
