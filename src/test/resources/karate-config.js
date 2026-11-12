function fn() {
    var env = karate.env;
    if (!env) {
        env = 'local';
    }
    var config = {
        env: env,
        baseUrl: karate.properties['baseUrl'] || 'http://localhost:8080'
    };
    karate.configure('connectTimeout', 10000);
    karate.configure('readTimeout', 10000);
    return config;
}