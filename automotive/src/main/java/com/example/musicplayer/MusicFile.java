package com.example.musicplayer;

import java.io.Serializable;
import java.io.File;
import java.util.Objects;

public class MusicFile implements Serializable {
    private String path;
    private String title;
    private String name;

    public MusicFile(String path) {
        this.path = path;
        this.title = new File(path).getName();
        this.name = new File(path).getName();
    }

    public String getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MusicFile musicFile = (MusicFile) o;
        return Objects.equals(path, musicFile.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
} 