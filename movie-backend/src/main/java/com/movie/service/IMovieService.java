package com.movie.service;

import org.springframework.data.domain.Page;

import com.baomidou.mybatisplus.extension.service.IService;
import com.movie.doc.MovieDoc;
import com.movie.dto.MovieDTO;
import com.movie.dto.SearchDTO;
import com.movie.entity.Info;
import com.movie.vo.MovieDetailVO;

/**
 * <p>
 * 电影信息表 服务类
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
public interface IMovieService extends IService<Info> {
    void addMovie(MovieDTO dto);
    
    Page<MovieDoc> search(SearchDTO dto);
     // 修改电影 (包含关联关系和ES同步)
    void updateMovie(MovieDTO dto);

    // 删除电影 (同步删除ES)
    void removeMovie(Long id);

    MovieDetailVO getMovieDetail(Long id);

    void syncEsData();
}
