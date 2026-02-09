import requests
import mysql.connector
import random
import datetime
from concurrent.futures import ThreadPoolExecutor, as_completed
from tqdm import tqdm

# ================= 配置区 =================

# 1. 数据库配置 (仅用于读取电影ID和影厅ID)
DB_CONFIG = {
    'host': 'localhost',
    'user': 'root',
    'password': '374629',  # 请确认密码
    'database': 'movie_db',
    'port': 3306,
}

# 2. 后端接口地址
API_BASE_URL = "http://localhost:8080"

# 3. 管理员账号 (必须是管理员才能排片)
ADMIN_USER = "admin"
ADMIN_PASS = "123456"

# 4. 每个电影生成多少场排片
SCHEDULES_PER_MOVIE = 5 

# 5. 并发线程数
MAX_WORKERS = 8

# =========================================

class ScheduleGenerator:
    def __init__(self):
        self.admin_token = None
        self.movie_ids = []
        self.cinema_hall_map = {} # {cinema_id: [hall_id, ...]}
        self.first_error_printed = False

    def get_db_connection(self):
        return mysql.connector.connect(**DB_CONFIG)

    def login_admin(self):
        """登录管理员获取 Token"""
        url = f"{API_BASE_URL}/user/login"
        try:
            res = requests.post(url, json={"username": ADMIN_USER, "password": ADMIN_PASS}, timeout=5)
            if res.status_code == 200:
                data = res.json()
                if data.get('code') == 200:
                    # 兼容 data 可能直接是对象或包含 token 字段
                    token = data.get('data', {}).get('token')
                    if token:
                        return token
                    # 有些后端封装直接把 token 放在 data 根目录下，视情况而定
                    return data.get('data')
            print(f"❌ 管理员登录失败，响应: {res.text}")
        except Exception as e:
            print(f"❌ 登录请求异常: {e}")
        return None

    def prepare_data(self):
        print("🔍 [1/2] 正在读取基础数据...")
        
        # 1. 登录
        self.admin_token = self.login_admin()
        if not self.admin_token:
            raise Exception("无法登录管理员，请检查后端是否启动或密码是否正确。")
        print("✅ 管理员登录成功")

        # 2. 读库
        conn = self.get_db_connection()
        cursor = conn.cursor()
        
        # 获取有效电影 (必须 duration > 0，否则后端计算结束时间会报空指针)
        cursor.execute("SELECT id FROM movie_info WHERE is_deleted = 0 AND duration IS NOT NULL AND duration > 0")
        self.movie_ids = [r[0] for r in cursor.fetchall()]
        
        # 获取影厅结构
        cursor.execute("SELECT cinema_id, id FROM cinema_hall")
        for cid, hid in cursor.fetchall():
            if cid not in self.cinema_hall_map:
                self.cinema_hall_map[cid] = []
            self.cinema_hall_map[cid].append(hid)
            
        cursor.close()
        conn.close()
        
        print(f"✅ 读取到: 有效电影 {len(self.movie_ids)} 部, 影院 {len(self.cinema_hall_map)} 家")
        
        if not self.movie_ids:
            raise Exception("未找到有效电影数据（请检查 duration 字段是否为空）")
        if not self.cinema_hall_map:
            raise Exception("未找到影院/影厅数据")

    def send_schedule_request(self, payload):
        url = f"{API_BASE_URL}/schedule/add"
        headers = {
            "Authorization": self.admin_token,
            "Content-Type": "application/json"
        }
        try:
            res = requests.post(url, json=payload, headers=headers, timeout=10)
            
            # 如果 HTTP 状态码不是 200 (例如 500 报错)
            if res.status_code != 200:
                try:
                    err = res.json()
                    return f"HTTP {res.status_code}: {err.get('error') or err.get('message')}"
                except:
                    return f"HTTP {res.status_code}: {res.text[:50]}..."

            # 业务状态码判断
            data = res.json()
            if data.get('code') == 200:
                return "success"
            else:
                return f"API Fail: {data.get('msg')}"
                
        except Exception as e:
            return f"Req Error: {str(e)}"

    def run(self):
        print(f"\n📅 [2/2] 开始生成排片 (每部电影 {SCHEDULES_PER_MOVIE} 场)...")
        tasks = []
        base_date = datetime.datetime.now()
        prices = [39.9, 45.0, 59.9, 68.0, 88.0]
        
        cinemas = list(self.cinema_hall_map.keys())

        # 构造任务列表
        for mid in self.movie_ids:
            for _ in range(SCHEDULES_PER_MOVIE):
                # 随机选影院和影厅
                cid = random.choice(cinemas)
                halls = self.cinema_hall_map[cid]
                if not halls: continue
                hid = random.choice(halls)
                
                # 随机时间 (未来 1-7 天, 早10点到晚10点)
                days = random.randint(1, 7)
                hours = random.randint(10, 22)
                mins = random.choice([0, 15, 30, 45])
                
                start_time = (base_date + datetime.timedelta(days=days)).replace(hour=hours, minute=mins, second=0)
                
                # 【关键修复】这里中间加了 'T'，适配 Java 的 LocalDateTime
                start_time_str = start_time.strftime("%Y-%m-%dT%H:%M:%S")

                payload = {
                    "cinemaId": cid,
                    "hallId": hid,
                    "movieId": mid,
                    "startTime": start_time_str, 
                    "price": random.choice(prices)
                }
                tasks.append(payload)

        print(f"🚀 准备向后端发送 {len(tasks)} 个排片请求...")
        
        success_cnt = 0
        conflict_cnt = 0
        error_cnt = 0
        
        # 多线程执行
        with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
            futures = [executor.submit(self.send_schedule_request, t) for t in tasks]
            
            pbar = tqdm(total=len(tasks), unit="req")
            for future in as_completed(futures):
                res = future.result()
                
                if res == "success":
                    success_cnt += 1
                elif "冲突" in res:
                    conflict_cnt += 1
                else:
                    error_cnt += 1
                    # 只打印第一个非冲突的错误，方便调试
                    if not self.first_error_printed:
                        tqdm.write(f"\n❌ [后端报错示例]: {res}")
                        self.first_error_printed = True
                        
                pbar.update(1)
            pbar.close()

        print("\n" + "="*40)
        print(f"✅ 执行结束统计:")
        print(f"   成功生成: {success_cnt}")
        print(f"   时间冲突: {conflict_cnt} (正常现象，跳过即可)")
        print(f"   其他错误: {error_cnt}")
        print("="*40)
        
        if error_cnt > 0:
            print("⚠️ 提示: 如果'其他错误'很多，请检查上方打印的[后端报错示例]。")
            print("   常见原因: 1. Token失效 2. 电影时长为空 3. 影厅ID不存在")

if __name__ == "__main__":
    try:
        generator = ScheduleGenerator()
        generator.prepare_data()
        generator.run()
    except Exception as e:
        print(f"\n❌ 脚本运行出错: {e}")
# import requests
# import mysql.connector
# import random
# import datetime
# import json
# from concurrent.futures import ThreadPoolExecutor, as_completed
# from tqdm import tqdm

# # ================= 配置区 =================

# # 1. 数据库配置 (用于读取ID)
# DB_CONFIG = {
#     'host': 'localhost',
#     'user': 'root',
#     'password': '374629',  # 替换你的密码
#     'database': 'movie_db',
#     'port': 3306,
# }

# # 2. 后端 API 地址
# API_BASE_URL = "http://localhost:8080"

# # 3. 账号配置
# ADMIN_USER = "admin"      # 管理员账号
# ADMIN_PASS = "123456"     # 管理员密码
# DEFAULT_USER_PASS = "123456" # 普通用户默认密码

# # 4. 并发数 (如果后端报错太频繁，可以调小这个数字)
# MAX_WORKERS = 8

# # =========================================

# # --- 真实语料库 ---
# POSITIVE_COMMENTS = [
#     "太好看了！全程无尿点，特效炸裂！", "剧情紧凑，演员演技在线，值得二刷。", "今年看过最棒的电影，强烈推荐！",
#     "结局太感人了，我在影院哭得稀里哗啦。", "视听盛宴！IMAX体验简直完美。", "剧本非常扎实，逻辑闭环，给编剧加鸡腿。",
#     "不愧是名导之作，镜头语言太美了。", "配乐满分，氛围感拉满。", "超出预期，比预告片还要精彩。",
#     "笑点密集，全场都在笑，解压神片。"
# ]
# NEUTRAL_COMMENTS = [
#     "整体还行，也就是爆米花电影水平吧。", "前半段有点拖沓，后半段比较精彩。", "中规中矩，适合周末打发时间。",
#     "特效不错，但是剧情有点硬伤。", "没有想象中那么好，但也不难看。", "演员颜值很高，但是演技有待提高。",
#     "及格分吧，随便看看还可以。", "结局有点仓促了，感觉没讲完。", "画面很美，但是故事没讲好。", "3D效果一般，建议看2D。"
# ]
# NEGATIVE_COMMENTS = [
#     "太失望了，剧情逻辑不通。", "睡着了三次，真的太无聊了。", "浪费票钱，避雷！",
#     "这就是在圈钱吧？五毛特效。", "台词尴尬，演员面瘫，看不下去。", "千万别看，毁原著！",
#     "看不懂在讲什么，剪辑混乱。", "全程尴尬，不知所云。", "今年最烂，没有之一。", "期望越大失望越大。"
# ]

# REPLIES = [
#     "确实，我也这么觉得。", "这就去买票！", "真的吗？本来还犹豫要不要看。", "同感！握手。",
#     "我觉得还行啊，没你说的那么差。", "你也太苛刻了吧。", "哈哈哈哈同感。", "必须二刷！",
#     "分析得太到位了。", "完全同意楼主的观点。", "我不这么认为，我觉得挺好看的。", "感谢排雷。"
# ]

# class DataGenerator:
#     def __init__(self):
#         self.admin_token = None
#         self.user_tokens = [] 
#         self.movie_ids = []
#         self.cinema_hall_map = {} 
#         self.first_error_printed = False # 控制只打印一次错误

#     def get_db_connection(self):
#         return mysql.connector.connect(**DB_CONFIG)

#     def login(self, username, password):
#         """登录获取Token"""
#         url = f"{API_BASE_URL}/user/login"
#         try:
#             res = requests.post(url, json={"username": username, "password": password}, timeout=5)
#             if res.status_code == 200:
#                 data = res.json()
#                 if data.get('code') == 200:
#                     # 兼容 data 可能直接是对象或包含 token 字段的情况
#                     return data.get('data', {}).get('token')
#         except:
#             pass
#         return None

#     def prepare_data(self):
#         print("🔍 [1/4] 准备基础数据...")
        
#         # 1. 管理员登录
#         self.admin_token = self.login(ADMIN_USER, ADMIN_PASS)
#         if not self.admin_token:
#             raise Exception(f"❌ 管理员登录失败！请检查后端是否启动，或账号密码是否正确。")
#         print("✅ 管理员登录成功")

#         # 2. 读取数据库
#         conn = self.get_db_connection()
#         cursor = conn.cursor()
        
#         # 读电影
#         cursor.execute("SELECT id FROM movie_info WHERE is_deleted = 0")
#         self.movie_ids = [r[0] for r in cursor.fetchall()]
        
#         # 读影厅 {cinema_id: [hall_id, ...]}
#         cursor.execute("SELECT cinema_id, id FROM cinema_hall")
#         for cid, hid in cursor.fetchall():
#             if cid not in self.cinema_hall_map:
#                 self.cinema_hall_map[cid] = []
#             self.cinema_hall_map[cid].append(hid)
            
#         # 读普通用户 (前100个)
#         cursor.execute("SELECT username FROM sys_user WHERE is_admin = 0 AND is_deleted = 0 LIMIT 100")
#         users = [r[0] for r in cursor.fetchall()]
        
#         cursor.close()
#         conn.close()
#         print(f"✅ 读取到: 电影 {len(self.movie_ids)} 部, 影院 {len(self.cinema_hall_map)} 家, 用户 {len(users)} 人")

#         # 3. 批量登录普通用户
#         print("🔓 正在预登录普通用户...")
#         with ThreadPoolExecutor(max_workers=10) as executor:
#             futures = [executor.submit(self.login, u, DEFAULT_USER_PASS) for u in users]
#             for f in as_completed(futures):
#                 token = f.result()
#                 if token:
#                     self.user_tokens.append(token)
        
#         if not self.user_tokens:
#             print("⚠️ 警告：没有普通用户登录成功，将无法生成评论！(请确认用户密码是否为 123456)")

#     # ================= 排片逻辑 =================

#     def send_schedule(self, payload):
#         url = f"{API_BASE_URL}/schedule/add"
#         headers = {
#             "Authorization": self.admin_token,
#             "Content-Type": "application/json"
#         }
#         try:
#             res = requests.post(url, json=payload, headers=headers, timeout=10)
#             data = res.json()
#             if data.get('code') == 200:
#                 return "success"
#             else:
#                 return f"fail: {data.get('msg')}"
#         except Exception as e:
#             return f"error: {str(e)}"

#     def generate_schedules(self):
#         print("\n📅 [2/4] 生成排片 (每部电影 5 场)...")
#         tasks = []
#         base_date = datetime.datetime.now()
#         prices = [39.9, 45.0, 59.9, 88.0]
        
#         cinemas = list(self.cinema_hall_map.keys())
#         if not cinemas:
#             print("❌ 错误：没有影院/影厅数据，跳过排片")
#             return

#         for mid in self.movie_ids:
#             for _ in range(5): # 每部电影生成5场
#                 cid = random.choice(cinemas)
#                 halls = self.cinema_hall_map[cid]
#                 if not halls: continue
#                 hid = random.choice(halls)
                
#                 # 随机生成未来 1-10 天的时间
#                 days = random.randint(1, 10)
#                 hours = random.randint(10, 22)
#                 mins = random.choice([0, 15, 30, 45])
                
#                 # 格式化时间，确保后端能解析
#                 start_time = (base_date + datetime.timedelta(days=days)).replace(hour=hours, minute=mins, second=0)
#                 start_time_str = start_time.strftime("%Y-%m-%d %H:%M:%S")

#                 payload = {
#                     "cinemaId": cid,
#                     "hallId": hid,
#                     "movieId": mid,
#                     "startTime": start_time_str,
#                     "price": random.choice(prices)
#                 }
#                 tasks.append(payload)

#         print(f"🚀 准备发送 {len(tasks)} 个排片请求...")
        
#         success = 0
#         conflict = 0
#         errors = 0
        
#         with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
#             futures = [executor.submit(self.send_schedule, t) for t in tasks]
            
#             pbar = tqdm(total=len(tasks))
#             for future in as_completed(futures):
#                 res = future.result()
#                 if res == "success":
#                     success += 1
#                 elif "冲突" in res:
#                     conflict += 1
#                 else:
#                     errors += 1
#                     if not self.first_error_printed:
#                         # 打印第一个错误供调试
#                         tqdm.write(f"\n❌ [排片报错样例]: {res}") 
#                         self.first_error_printed = True
#                 pbar.update(1)
#             pbar.close()
            
#         print(f"✅ 排片统计: 成功 {success}, 冲突跳过 {conflict}, 其他失败 {errors}")
#         if errors > 0:
#             print("⚠️ 如果'其他失败'很多，请检查控制台打印的报错样例，可能是日期格式或Token失效问题。")

#     # ================= 评论逻辑 =================

#     def send_review(self, token, payload):
#         url = f"{API_BASE_URL}/review/add"
#         headers = {"Authorization": token}
#         try:
#             requests.post(url, json=payload, headers=headers, timeout=5)
#         except: pass

#     def generate_reviews(self):
#         if not self.user_tokens: return
#         print("\n💬 [3/4] 生成真人感评论...")
        
#         tasks = []
#         for mid in self.movie_ids:
#             # 每部电影 随机 5-10 条评论
#             count = random.randint(5, 10)
#             for _ in range(count):
#                 token = random.choice(self.user_tokens)
                
#                 # 随机决定 好评/中评/差评
#                 rand = random.random()
#                 if rand < 0.6: # 60% 好评
#                     content = random.choice(POSITIVE_COMMENTS)
#                     score = round(random.uniform(8.0, 10.0), 1)
#                 elif rand < 0.9: # 30% 中评
#                     content = random.choice(NEUTRAL_COMMENTS)
#                     score = round(random.uniform(5.0, 7.9), 1)
#                 else: # 10% 差评
#                     content = random.choice(NEGATIVE_COMMENTS)
#                     score = round(random.uniform(1.0, 4.9), 1)
                
#                 payload = {"movieId": mid, "score": score, "content": content}
#                 tasks.append((token, payload))

#         with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
#             futures = [executor.submit(self.send_review, t[0], t[1]) for t in tasks]
#             for _ in tqdm(as_completed(futures), total=len(tasks), desc="Posting Reviews"):
#                 pass

#     # ================= 回复逻辑 =================

#     def send_reply(self, token, payload):
#         url = f"{API_BASE_URL}/reply/add"
#         headers = {"Authorization": token}
#         try:
#             requests.post(url, json=payload, headers=headers, timeout=5)
#         except: pass

#     def generate_replies(self):
#         if not self.user_tokens: return
#         print("\n🗣️ [4/4] 生成评论回复...")
        
#         # 查出现有评论
#         conn = self.get_db_connection()
#         cursor = conn.cursor()
#         cursor.execute("SELECT id, user_id FROM movie_review")
#         reviews = cursor.fetchall()
#         cursor.close()
#         conn.close()

#         tasks = []
#         for r_id, author_id in reviews:
#             # 20% 概率产生回复
#             if random.random() < 0.2:
#                 token = random.choice(self.user_tokens)
#                 content = random.choice(REPLIES)
#                 payload = {
#                     "reviewId": r_id,
#                     "content": content,
#                     "targetUserId": author_id
#                 }
#                 tasks.append((token, payload))

#         with ThreadPoolExecutor(max_workers=MAX_WORKERS) as executor:
#             futures = [executor.submit(self.send_reply, t[0], t[1]) for t in tasks]
#             for _ in tqdm(as_completed(futures), total=len(tasks), desc="Posting Replies"):
#                 pass

# if __name__ == "__main__":
#     gen = DataGenerator()
#     try:
#         gen.prepare_data()
#         gen.generate_schedules()
#         gen.generate_reviews()
#         gen.generate_replies()
#         print("\n🎉 所有数据生成完毕！")
#     except Exception as e:
#         print(f"\n❌ 发生致命错误: {e}")