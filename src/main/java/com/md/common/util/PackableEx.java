package com.md.common.util;


public interface PackableEx extends Packable {
    void unmarshal(ByteBuf in);
}
