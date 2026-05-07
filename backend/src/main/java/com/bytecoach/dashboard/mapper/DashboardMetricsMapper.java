package com.bytecoach.dashboard.mapper;

import com.bytecoach.dashboard.dto.RecentInterviewVO;
import com.bytecoach.dashboard.dto.WeakPointVO;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface DashboardMetricsMapper {

    @Select("""
            SELECT COUNT(*)
            FROM chat_session
            WHERE user_id = #{userId}
            """)
    Long countChatSessions(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM interview_session
            WHERE user_id = #{userId}
            """)
    Long countInterviewSessions(@Param("userId") Long userId);

    @Select("""
            SELECT COALESCE(ROUND(AVG(score), 2), 0)
            FROM interview_record
            WHERE user_id = #{userId}
            """)
    BigDecimal averageInterviewScore(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM wrong_question
            WHERE user_id = #{userId}
            """)
    Long countWrongQuestions(@Param("userId") Long userId);

    @Select("""
            SELECT id AS session_id,
                   direction,
                   COALESCE(total_score, 0) AS total_score,
                   status,
                   COALESCE(end_time, update_time) AS finished_at
            FROM interview_session
            WHERE user_id = #{userId}
            ORDER BY COALESCE(end_time, update_time) DESC
            LIMIT 3
            """)
    List<RecentInterviewVO> selectRecentInterviews(@Param("userId") Long userId);

    @Select("""
            SELECT COALESCE(c.name, '未分类') AS category_name,
                   COUNT(w.id) AS wrong_count,
                   COALESCE(ROUND(AVG(ir.score), 2), 0) AS score
            FROM wrong_question w
            LEFT JOIN question q ON q.id = w.question_id
            LEFT JOIN category c ON c.id = q.category_id
            LEFT JOIN interview_record ir
                   ON ir.user_id = w.user_id
                  AND ir.question_id = w.question_id
            WHERE w.user_id = #{userId}
            GROUP BY c.id, c.name
            ORDER BY wrong_count DESC, score ASC
            LIMIT 5
            """)
    List<WeakPointVO> selectWeakPoints(@Param("userId") Long userId);
}
