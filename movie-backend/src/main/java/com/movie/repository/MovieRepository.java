package com.movie.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.movie.doc.MovieDoc;

public interface MovieRepository extends ElasticsearchRepository<MovieDoc, Long> {
    // Spring Data Elasticsearch 会自动帮我们实现 CRUD 方法
}