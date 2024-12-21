package com.Farouk.security.storge.Data;


import com.Farouk.security.user.Data.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
@Setter
@Getter
@Entity
public class UploadedFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String link;
    private String fileName;
    private Date date;
    private Float size;
    @JsonIgnore
    private String keywords;
    @Enumerated(EnumType.STRING)
    private FileType fileType;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;



}
