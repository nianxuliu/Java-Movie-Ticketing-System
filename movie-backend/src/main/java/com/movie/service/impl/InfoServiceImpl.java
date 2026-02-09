package com.movie.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.movie.doc.MovieDoc;
import com.movie.dto.MovieDTO;
import com.movie.dto.SearchDTO;
import com.movie.entity.Actor;
import com.movie.entity.ActorInfo;
import com.movie.entity.Director;
import com.movie.entity.DirectorInfo;
import com.movie.entity.Info;
import com.movie.mapper.InfoMapper;
import com.movie.repository.MovieRepository;
import com.movie.service.IActorInfoService;
import com.movie.service.IActorService; // 引入新的 Query 类
import com.movie.service.IDirectorInfoService;
import com.movie.service.IDirectorService;
import com.movie.service.IMovieService;
import com.movie.vo.MovieDetailVO;

import cn.hutool.core.bean.BeanUtil;

/**
 * <p>
 * 电影信息表 服务实现类
 * </p>
 *
 * @author Liu
 * @since 2025-12-25
 */
@Service
public class InfoServiceImpl extends ServiceImpl<InfoMapper, Info> implements IMovieService {
    // 注入关联表服务 (注意名字：IActorService 对应 movie_actor 表)
    @Autowired
    private IActorService actorRelationService; 

    // 注入关联表服务 (注意名字：IDirectorService 对应 movie_director 表)
    @Autowired
    private IDirectorService directorRelationService;

    //注入 ES Repository ---
    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private IActorInfoService actorInfoService;

    @Autowired
    private IDirectorInfoService directorInfoService;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Override
    @Transactional(rollbackFor = Exception.class) // 开启事务，任何一步报错都回滚
    public void addMovie(MovieDTO dto) {
        // 1. 保存电影基本信息 (Info 表)
        Info movie = new Info();
        BeanUtil.copyProperties(dto, movie);
        
        // 初始化统计数据
        movie.setRating(BigDecimal.ZERO); 
        movie.setReviewCount(0);
        // 如果数据库有默认值这行可以省，为了保险起见设为false
        movie.setIsDeleted(false); 
        
        this.save(movie); // 保存到数据库，自动生成 ID
        Long movieId = movie.getId(); // 获取新生成的电影 ID

        // 2. 保存演员关联 (Actor 表: movie_actor)
        if (dto.getActorIds() != null && !dto.getActorIds().isEmpty()) {
            for (Long actorId : dto.getActorIds()) {
                Actor relation = new Actor(); // 这是关联对象
                relation.setMovieId(movieId);
                relation.setActorId(actorId);
                actorRelationService.save(relation);
            }
        }

        // 3. 保存导演关联 (Director 表: movie_director)
        if (dto.getDirectorIds() != null && !dto.getDirectorIds().isEmpty()) {
            for (Long directorId : dto.getDirectorIds()) {
                Director relation = new Director(); // 这是关联对象
                relation.setMovieId(movieId);
                relation.setDirectorId(directorId);
                directorRelationService.save(relation);
            }
        }

        // 【数据同步到 Elasticsearch】
        // 4. 根据 ID 查出演员名和导演名
        List<String> actorNames = Collections.emptyList();
        if (dto.getActorIds() != null && !dto.getActorIds().isEmpty()) {
            actorNames = actorInfoService.listByIds(dto.getActorIds())
                    .stream().map(ActorInfo::getName).collect(Collectors.toList());
        }

        List<String> directorNames = Collections.emptyList();
        if (dto.getDirectorIds() != null && !dto.getDirectorIds().isEmpty()) {
            directorNames = directorInfoService.listByIds(dto.getDirectorIds())
                    .stream().map(DirectorInfo::getName).collect(Collectors.toList());
        }

        // 5. 组装 MovieDoc 对象
        MovieDoc doc = new MovieDoc();
        BeanUtil.copyProperties(dto, doc); // 把 dto 的 title, genre 等信息拷过来
        doc.setId(movieId); // 设置 ES 文档的 ID
        doc.setPosterUrl(dto.getPosterUrl()); // 显式赋值海报
        doc.setActors(actorNames);
        doc.setDirectors(directorNames);
        if (dto.getReleaseDate() != null) doc.setReleaseDate(dto.getReleaseDate().toString());
        doc.setRating(0.0); 

        // 6. 保存到 ES
        movieRepository.save(doc);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Page<MovieDoc> search(SearchDTO dto) {
        // 1. 构建分页请求 (这部分不变)
        PageRequest pageRequest = PageRequest.of(dto.getPage() - 1, dto.getSize());

        // 2. 构建 ES 查询条件 (Spring Boot 3.x 全新写法)
        Query query = NativeQuery.builder()
                .withQuery(q -> q
                        .multiMatch(mq -> mq
                                .query(dto.getKeyword())
                                .fields("title", "originalTitle", "actors", "directors", "synopsis")
                        )
                )
                .withPageable(pageRequest)
                .build();

        // 3. 执行查询 (这部分不变)
        SearchHits<MovieDoc> searchHits = elasticsearchOperations.search(query, MovieDoc.class);
        
        // 4. 转换成分页对象 (这部分不变)
        SearchPage<MovieDoc> searchPage = SearchHitSupport.searchPageFor(searchHits, pageRequest);

        return (Page<MovieDoc>) SearchHitSupport.unwrapSearchHits(searchPage);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMovie(MovieDTO dto) {
        // 1. 更新 MySQL 主表
        Info movie = new Info();
        BeanUtil.copyProperties(dto, movie);
        // 如果 dto.id 为空，抛异常
        if (movie.getId() == null) throw new RuntimeException("修改必须指定ID");
        this.updateById(movie);

        // 2. 更新关联关系 (先删后加，这是处理多对多更新最稳妥的策略)
        // 2.1 删除旧的演员关联
        QueryWrapper<Actor> actorDeleteWrapper = new QueryWrapper<>();
        actorDeleteWrapper.eq("movie_id", movie.getId());
        actorRelationService.remove(actorDeleteWrapper);
        
        // 2.2 删除旧的导演关联
        QueryWrapper<Director> directorDeleteWrapper = new QueryWrapper<>();
        directorDeleteWrapper.eq("movie_id", movie.getId());
        directorRelationService.remove(directorDeleteWrapper);

        // 2.3 插入新的演员关联
        if (dto.getActorIds() != null && !dto.getActorIds().isEmpty()) {
            for (Long actorId : dto.getActorIds()) {
                Actor relation = new Actor();
                relation.setMovieId(movie.getId());
                relation.setActorId(actorId);
                actorRelationService.save(relation);
            }
        }

        // 2.4 插入新的导演关联
        if (dto.getDirectorIds() != null && !dto.getDirectorIds().isEmpty()) {
            for (Long directorId : dto.getDirectorIds()) {
                Director relation = new Director();
                relation.setMovieId(movie.getId());
                relation.setDirectorId(directorId);
                directorRelationService.save(relation);
            }
        }

        // 3. 同步更新 Elasticsearch
        // 查出最新的名字
        List<String> actorNames = Collections.emptyList();
        if (dto.getActorIds() != null && !dto.getActorIds().isEmpty()) {
            actorNames = actorInfoService.listByIds(dto.getActorIds())
                    .stream().map(ActorInfo::getName).collect(Collectors.toList());
        }
        List<String> directorNames = Collections.emptyList();
        if (dto.getDirectorIds() != null && !dto.getDirectorIds().isEmpty()) {
            directorNames = directorInfoService.listByIds(dto.getDirectorIds())
                    .stream().map(DirectorInfo::getName).collect(Collectors.toList());
        }

        MovieDoc doc = new MovieDoc();
        BeanUtil.copyProperties(dto, doc); // 此时 dto 里有 id
        doc.setPosterUrl(dto.getPosterUrl());
        doc.setActors(actorNames);
        doc.setDirectors(directorNames);
        if (dto.getReleaseDate() != null) doc.setReleaseDate(dto.getReleaseDate().toString());
        
        // save 方法在 ES 里是 "Upsert" (有则更新，无则新增)
        movieRepository.save(doc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMovie(Long id) {
        // 1. MySQL 逻辑删除 (因为加了 @TableLogic)
        this.removeById(id);

        // 2. ES 物理删除 (搜不到才是目的)
        movieRepository.deleteById(id);
    }

    @Override
    public MovieDetailVO getMovieDetail(Long id) {
        // 1. 查电影主表
        Info movie = this.getById(id);
        if (movie == null) throw new RuntimeException("电影不存在");

        MovieDetailVO vo = new MovieDetailVO();
        BeanUtil.copyProperties(movie, vo);

        // 2. 查演员关联关系 (movie_actor) -> 拿到 actorId 列表
        QueryWrapper<Actor> actorQuery = new QueryWrapper<>();
        actorQuery.eq("movie_id", id);
        List<Actor> actorRelations = actorRelationService.list(actorQuery);
        
        if (!actorRelations.isEmpty()) {
            List<Long> actorIds = actorRelations.stream().map(Actor::getActorId).collect(Collectors.toList());
            // 3. 查演员详情 (actor_info)
            List<ActorInfo> actorInfos = actorInfoService.listByIds(actorIds);
            vo.setActorList(actorInfos);
        }

        // 4. 查导演关联关系 (movie_director) -> 拿到 directorId 列表
        QueryWrapper<Director> directorQuery = new QueryWrapper<>();
        directorQuery.eq("movie_id", id);
        List<Director> directorRelations = directorRelationService.list(directorQuery);
        
        if (!directorRelations.isEmpty()) {
            List<Long> directorIds = directorRelations.stream().map(Director::getDirectorId).collect(Collectors.toList());
            // 5. 查导演详情 (director_info)
            List<DirectorInfo> directorInfos = directorInfoService.listByIds(directorIds);
            vo.setDirectorList(directorInfos);
        }

        return vo;
    }

    @Override
    public void syncEsData() {
        // 1. 先清空 ES 中的旧数据 (防止重复或脏数据)
        movieRepository.deleteAll();

        // 2. 查出 MySQL 里所有的电影
        List<Info> allMovies = this.list();
        if (allMovies.isEmpty()) return;

        List<MovieDoc> docs = new ArrayList<>();

        // 3. 遍历每一部电影，组装数据
        for (Info movie : allMovies) {
            MovieDoc doc = new MovieDoc();
            BeanUtil.copyProperties(movie, doc);

            doc.setPosterUrl(movie.getPosterUrl()); // 确保海报地址存入
            if (movie.getRating() != null) {
                doc.setRating(movie.getRating().doubleValue()); // BigDecimal 转为 Double
            }
            if (movie.getReleaseDate() != null) {
                doc.setReleaseDate(movie.getReleaseDate().toString()); // LocalDate 转为 String
            }

            // --- 查演员名字 ---
            // 查中间表
            QueryWrapper<Actor> actorQuery = new QueryWrapper<>();
            actorQuery.eq("movie_id", movie.getId());
            List<Actor> actorRelations = actorRelationService.list(actorQuery);
            
            List<String> actorNames = new ArrayList<>();
            if (!actorRelations.isEmpty()) {
                List<Long> actorIds = actorRelations.stream().map(Actor::getActorId).collect(Collectors.toList());
                // 查信息表
                List<ActorInfo> actors = actorInfoService.listByIds(actorIds);
                actorNames = actors.stream().map(ActorInfo::getName).collect(Collectors.toList());
            }
            doc.setActors(actorNames);

            // --- 查导演名字 ---
            QueryWrapper<Director> directorQuery = new QueryWrapper<>();
            directorQuery.eq("movie_id", movie.getId());
            List<Director> directorRelations = directorRelationService.list(directorQuery);

            List<String> directorNames = new ArrayList<>();
            if (!directorRelations.isEmpty()) {
                List<Long> directorIds = directorRelations.stream().map(Director::getDirectorId).collect(Collectors.toList());
                List<DirectorInfo> directors = directorInfoService.listByIds(directorIds);
                directorNames = directors.stream().map(DirectorInfo::getName).collect(Collectors.toList());
            }
            doc.setDirectors(directorNames);

            // 加入待保存列表
            docs.add(doc);
        }

        // 4. 批量保存到 ES (性能比一条条存快得多)
        movieRepository.saveAll(docs);
    }
}
