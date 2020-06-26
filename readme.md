- 뿌리기 API

토큰 중복되지 않게 TOKEN을 primary key로 설정하여 다른 서버에서 먼저 insert가 될경우 실패하여 새로운 토큰으로 실행하게 하였습니다. 시간이 부족하여 어플리케이션단에서 토큰을 생성하고 insert 시도를 하지만 해당 부분은 DB에서 직접 생성후 해당 값을 return해주도록 FUNCTION을 만들어서 사용할 경우 DB connection 회수를 줄일 수 있을것으로 생각됩니다.

- 받기 API

분배되지 않은 건의 token과 ID를 기준으로 receiver를 업데이트 합니다.
이때도 마찬가지로 다른 서버에서 먼저 업데이트 될 경우 결과에 따라 다음 ID를 다시 받아 업데이트 하도록 하였습니다.
Update 의 inner 쿼리에 limit가 지원할 경우 업데이트 후 token과 receiver를 기준으로 select를 할 경우 한번의 query로 해결할 수 있게 됩니다.