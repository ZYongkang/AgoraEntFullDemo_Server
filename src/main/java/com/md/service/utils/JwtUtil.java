package com.md.service.utils;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.token.secret}")
    private String secretKey;

    @Value("${jwt.token.exp-time}")
    private String exTime;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 用户登录成功后生成Jwt
     * 使用Hs256算法  私匙使用用户密码
     *
     * @param
     * @return
     */
    public String createJWT(String userNo) {
        //指定签名的时候使用的签名算法，也就是header那部分，jjwt已经将这部分内容封装好了。
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        //创建payload的私有声明（根据特定的业务需要添加，如果要拿这个做验证，一般是需要和jwt的接收方提前沟通好验证方式的）
        Map<String, Object> claims = new HashMap<String, Object>();
        claims.put("user_no", userNo);
        //生成签名的时候使用的秘钥secret,这个方法本地封装了的，一般可以从本地配置文件中读取，切记这个秘钥不能外露哦。它就是你服务端的私钥，在任何场景都不应该流露出去。一旦客户端得知这个secret, 那就意味着客户端是可以自我签发jwt了。
        String key = Base64.getEncoder().encodeToString(secretKey.getBytes());
        //下面就是在为payload添加各种标准声明和私有声明了
        //这里其实就是new一个JwtBuilder，设置jwt的body
        JwtBuilder builder = Jwts.builder()
                //如果有私有声明，一定要先设置这个自己创建的私有的声明，这个是给builder的claim赋值，一旦写在标准的声明赋值之后，就是覆盖了那些标准的声明的
                .setClaims(claims)
                //设置jti(JWT ID)：是JWT的唯一标识，根据业务需要，这个可以设置为一个不重复的值，主要用来作为一次性token,从而回避重放攻击。
                .setId(UUID.randomUUID().toString())
                //iat: jwt的签发时间
                .setIssuedAt(new Date())
                //代表这个JWT的主体，即它的所有人，这个是一个json格式的字符串，可以存放什么userid，roldid之类的，作为什么用户的唯一标志。
                .setSubject(userNo)
                //设置签名使用的签名算法和签名使用的秘钥
                .signWith(signatureAlgorithm, key);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 10);
        Date exp = calendar.getTime();
        builder.setExpiration(exp);
        String result = builder.compact();
        String tokenKey = "user_token:"+userNo;
        redisTemplate.opsForValue().set(tokenKey,result,Integer.parseInt(exTime), TimeUnit.DAYS);
        return result;
    }

    public String getUserNo(String token) {
        if(StringUtils.isBlank(token)){
            return null;
        }
        Claims claims = parseJWT(token);
        if(claims==null){
            return null;
        }
        return claims.get("user_no", String.class);
    }

    private Claims parseJWT(String token) {
        Claims claims = null;
        try {
            if (StringUtils.isNotBlank(token)) {
                //解析jwt
                String key = Base64.getEncoder().encodeToString(secretKey.getBytes());
                claims = Jwts.parser().setSigningKey(key)
                        .parseClaimsJws(token).getBody();
            }else {
                log.warn("token is empty!");
            }
        } catch (Exception e) {
            log.error("token parse failed | cause:", e);
        }
        return claims;
    }

    public static void main(String[] args) {
        System.out.println(UUID.randomUUID().toString().toLowerCase().replaceAll("-", ""));
    }

}
