use std::env;
use std::fs;
use std::path::Path;

extern crate prost_build;

fn main() {
    if !Path::new("src/googleplay.rs").exists() {
        prost_build::compile_protos(&["protos/GooglePlay.proto"],
                                    &["protos/"]).unwrap();

        let out_dir = env::var("OUT_DIR").unwrap();
        let src_path = format!("{}/_.rs", out_dir);
        fs::rename(src_path, "src/googleplay.rs").expect("fs::rename error");
    }
}
