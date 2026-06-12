package com.gamevault.dto;

/** Body for creating a "post" activity on a catalog game's page (text and/or image). */
public record PostRequest(String text, String image) {}
