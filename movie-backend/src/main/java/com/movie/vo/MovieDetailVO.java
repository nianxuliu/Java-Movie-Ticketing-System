package com.movie.vo;

import java.util.List;

import com.movie.entity.ActorInfo;
import com.movie.entity.DirectorInfo;
import com.movie.entity.Info;

public class MovieDetailVO extends Info {
    // 除了继承电影的基本信息，还需要包含具体的演员和导演对象列表
    private List<ActorInfo> actorList;
    private List<DirectorInfo> directorList;

    // 手动生成 Getter/Setter
    public List<ActorInfo> getActorList() { return actorList; }
    public void setActorList(List<ActorInfo> actorList) { this.actorList = actorList; }
    public List<DirectorInfo> getDirectorList() { return directorList; }
    public void setDirectorList(List<DirectorInfo> directorList) { this.directorList = directorList; }
}