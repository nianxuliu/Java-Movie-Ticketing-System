import mysql.connector
import requests
import time
from tqdm import tqdm

# ================= 配置区 =================
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '374629', # 修改为你的密码
    'database': 'movie_db',
    'port': 3306,
    'charset': 'utf8mb4'
}

TMDB_API_KEY = "405c051b140a9bbb697b1e5dcc423a32" # <--- 必填
TMDB_LANG = "zh-CN"
TARGET_COUNT = 5000  # 目标抓取数量

# =========================================

def get_conn():
    return mysql.connector.connect(**DB_CONFIG)

def get_existing_data(cursor):
    """预加载所有已存在的电影、演员、导演，防止重复插入"""
    print("🔍 正在预加载现有数据以进行去重...")
    
    # 电影标题集合
    cursor.execute("SELECT title FROM movie_info")
    existing_titles = {row[0] for row in cursor.fetchall()}
    
    # 演员名 -> ID 映射
    cursor.execute("SELECT name, id FROM actor_info")
    actor_map = {row[0]: row[1] for row in cursor.fetchall()}
    
    # 导演名 -> ID 映射
    cursor.execute("SELECT name, id FROM director_info")
    director_map = {row[0]: row[1] for row in cursor.fetchall()}
    
    print(f"✅ 已加载: 电影 {len(existing_titles)} 部, 演员 {len(actor_map)} 人, 导演 {len(director_map)} 人")
    return existing_titles, actor_map, director_map

def insert_person(cursor, person_data, is_director, cache_map):
    """插入演员或导演，如果已存在则返回ID"""
    name = person_data['name']
    
    # 1. 查缓存
    if name in cache_map:
        return cache_map[name]
    
    # 2. 准备数据
    en_name = person_data.get('original_name', '')
    profile = person_data.get('profile_path')
    avatar = f"https://image.tmdb.org/t/p/w200{profile}" if profile else ""
    gender = 1 if person_data.get('gender') == 2 else 0 # 2是男
    
    # 3. 插入数据库
    if is_director:
        sql = "INSERT INTO director_info (name, en_name, gender, avatar_url) VALUES (%s, %s, %s, %s)"
        cursor.execute(sql, (name, en_name, gender, avatar))
    else:
        sql = "INSERT INTO actor_info (name, en_name, gender, avatar_url) VALUES (%s, %s, %s, %s)"
        cursor.execute(sql, (name, en_name, gender, avatar))
        
    new_id = cursor.lastrowid
    cache_map[name] = new_id # 更新缓存
    return new_id

def main():
    if TMDB_API_KEY == "你的_TMDB_API_KEY":
        print("❌ 请填入 API KEY")
        return

    conn = get_conn()
    cursor = conn.cursor()
    
    try:
        # 1. 加载现有数据去重
        existing_titles, actor_map, director_map = get_existing_data(cursor)
        
        fetched_count = 0
        page = 1
        
        # 进度条
        pbar = tqdm(total=TARGET_COUNT, desc="抓取进度", unit="部")
        
        while fetched_count < TARGET_COUNT:
            try:
                # 获取热门列表
                list_url = f"https://api.themoviedb.org/3/movie/popular?api_key={TMDB_API_KEY}&language={TMDB_LANG}&page={page}"
                res = requests.get(list_url, timeout=10)
                if res.status_code != 200:
                    print(f"⚠️ 页面 {page} 请求失败，跳过")
                    page += 1
                    continue
                
                movies = res.json().get('results', [])
                if not movies:
                    print("⚠️ 没有更多电影了")
                    break
                
                for m in movies:
                    if fetched_count >= TARGET_COUNT: break
                    
                    title = m.get('title')
                    
                    # --- 核心去重逻辑 ---
                    if title in existing_titles:
                        continue 
                    
                    # 获取详情
                    detail_url = f"https://api.themoviedb.org/3/movie/{m['id']}?api_key={TMDB_API_KEY}&language={TMDB_LANG}&append_to_response=credits"
                    detail_res = requests.get(detail_url, timeout=10)
                    if detail_res.status_code != 200: continue
                    detail = detail_res.json()
                    
                    # 1. 插入电影
                    poster = f"https://image.tmdb.org/t/p/w500{detail.get('poster_path')}" if detail.get('poster_path') else ""
                    genre = detail['genres'][0]['name'] if detail.get('genres') else "剧情"
                    country = detail['production_countries'][0]['name'] if detail.get('production_countries') else "未知"
                    rating = detail.get('vote_average', 0.0)
                    
                    sql_movie = """
                        INSERT INTO movie_info 
                        (title, original_title, release_date, duration, genre, language, country, synopsis, poster_url, rating)
                        VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    """
                    cursor.execute(sql_movie, (
                        title, detail.get('original_title'), 
                        detail.get('release_date') or '2020-01-01',
                        detail.get('runtime') or 100, genre, "英语/多语言", country,
                        detail.get('overview', '')[:2000], poster, rating
                    ))
                    movie_id = cursor.lastrowid
                    existing_titles.add(title) # 加入去重集合
                    
                    # 2. 处理人员关联
                    credits = detail.get('credits', {})
                    
                    # 导演 (取前1个)
                    crew = credits.get('crew', [])
                    directors = [p for p in crew if p['job'] == 'Director']
                    for d in directors[:1]:
                        d_id = insert_person(cursor, d, True, director_map)
                        # 插入关联
                        try:
                            cursor.execute("INSERT INTO movie_director (movie_id, director_id) VALUES (%s, %s)", (movie_id, d_id))
                        except: pass # 忽略重复关联
                        
                    # 演员 (取前5个)
                    cast = credits.get('cast', [])
                    for a in cast[:5]:
                        a_id = insert_person(cursor, a, False, actor_map)
                        # 插入关联
                        try:
                            cursor.execute("INSERT INTO movie_actor (movie_id, actor_id, role_name) VALUES (%s, %s, %s)", (movie_id, a_id, a.get('character', '')))
                        except: pass

                    fetched_count += 1
                    pbar.update(1)
                    
                    # 每 50 部提交一次，防止脚本中断数据丢失
                    if fetched_count % 50 == 0:
                        conn.commit()
                        
                page += 1
                
            except Exception as e:
                print(f"\n❌ 发生异常 (自动重试): {e}")
                time.sleep(3) # 休息一下再试
                
    finally:
        conn.commit() # 最后提交一次
        cursor.close()
        conn.close()
        print("\n😴 任务结束，你可以安心睡觉了！")

if __name__ == "__main__":
    main()