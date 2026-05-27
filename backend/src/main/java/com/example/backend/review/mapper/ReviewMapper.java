package com.example.backend.review.mapper;

import com.example.backend.review.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewMapper {

    int insert(Review review);

    Review selectById(@Param("id") Long id);

    List<Review> selectByOrderId(@Param("orderId") Long orderId);

    List<Review> selectByRevieweeId(@Param("revieweeId") Long revieweeId);

    List<Review> selectByReviewerId(@Param("reviewerId") Long reviewerId);

    Double selectAvgRatingByUserId(@Param("userId") Long userId);

    int countByUserId(@Param("userId") Long userId);

    boolean existsByOrderIdAndReviewerId(@Param("orderId") Long orderId, @Param("reviewerId") Long reviewerId);
}
