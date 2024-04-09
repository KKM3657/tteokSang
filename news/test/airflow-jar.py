from airflow import DAG
from airflow.operators.bash import BashOperator
from datetime import datetime, timedelta
from airflow.models import Variable


# DAG 정의
dag = DAG(
    'make_season_directory',
    start_date=datetime(2024,4,2),
    description = 'Start half season statistics',
    # schedule_interval = timedelta(days = 1)
    schedule = timedelta(days = 1)
)

# 시즌이랑 턴 번호를 주는데요? 180으로 나누면 반기 번호가 나오지요
season_id = Variable.get("season_id", default_var = 1)
half_season_id = int(Variable.get("turn", default_var = 180) / 180)
out_path = f'out/{season_id}/{half_season_id}'

# 받은 폴더 이름으로 Bash 명령어 생헝
bash_command = f'hdfs dfs -mkdir -p hdfs://localhost:9000/user/hdfs/{season_id}'

bash_command_put_data = f'hdfs dfs -put -f /home/hdfs/logs/{season_id}/{half_season_id} hdfs://localhost:9000/user/hdfs/{season_id}'

# jar 파일 실행을 위한 경로 지정 및 input/output 폴더 지정
bash_command_jar = f'hadoop jar /home/hdfs/jars/hadoop.jar com.welcome.Main hdfs://localhost:9000/user/hdfs/{season_id}/{half_season_id} hdfs://0.0.0.0:9000/user/{out_path} -libjars /home/hdfs/jars/kafka-clients.jar'

# 디렉토리 만드는 task
create_directory_task = BashOperator(
    task_id='create_directory',
    bash_command = bash_command,
    dag = dag
)

# 데이터 넣어주는 task
put_data_task = BashOperator(
    task_id = 'put_data',
    bash_command = bash_command_put_data,
    dag = dag
)

# 맵리듀스 실행하는 task
run_mapreduce_jar = BashOperator(
    task_id = 'run_mapreduce_jar',
    bash_command = bash_command_jar,
    dag = dag
)

# 작업간 의존성
create_directory_task >> put_data_task >> run_mapreduce_jar