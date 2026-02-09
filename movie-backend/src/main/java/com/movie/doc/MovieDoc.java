package com.movie.doc;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "movie_index") // 定义 ES 索引名
public class MovieDoc {
    @Id
    private Long id; // 对应 movie_info 表的 ID

    // ik_max_word 是中文分词器，ik_smart 是更智能的
    // analyzer 是写入时分词，searchAnalyzer 是搜索时分词
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String originalTitle;

    @Field(type = FieldType.Keyword) // Keyword 不分词，精确匹配
    private String genre;

    @Field(type = FieldType.Keyword)
    private String country;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String synopsis;

    @Field(type = FieldType.Keyword)
    private String posterUrl; // 海报地址

    @Field(type = FieldType.Double)
    private Double rating;    // 评分

    @Field(type = FieldType.Keyword)
    private String releaseDate; // 上映日期（存字符串即可，方便展示）
    
    // --- 关键：把关联信息也存进来 ---
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private List<String> actors; // 演员名列表 ["成龙", "吴彦祖"]

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private List<String> directors; // 导演名列表

    // --- 请手动生成 Getter / Setter ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOriginalTitle() { return originalTitle; }
    public void setOriginalTitle(String originalTitle) { this.originalTitle = originalTitle; }
    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getSynopsis() { return synopsis; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }
    public List<String> getActors() { return actors; }
    public void setActors(List<String> actors) { this.actors = actors; }
    public List<String> getDirectors() { return directors; }
    public void setDirectors(List<String> directors) { this.directors = directors; }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(String releaseDate) {
        this.releaseDate = releaseDate;
    }
}