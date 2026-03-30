package com.lab.mongodbcdckafka.service

import com.lab.mongodbcdckafka.domain.ScopeFunctionGuide
import org.springframework.stereotype.Service

@Service
class ScopeFunctionGuideService {

    fun guides(): List<ScopeFunctionGuide> = listOf(
        ScopeFunctionGuide(
            name = "let",
            primaryUseCase = "nullable 값 처리나 짧은 범위의 변환에 사용합니다.",
            caution = "과도한 중첩은 it 체인을 만들어 가독성을 해칩니다.",
        ),
        ScopeFunctionGuide(
            name = "run",
            primaryUseCase = "객체 컨텍스트에서 계산한 결과를 반환할 때 사용합니다.",
            caution = "this 기반 문맥이 길어지면 외부 스코프와 혼동될 수 있습니다.",
        ),
        ScopeFunctionGuide(
            name = "apply",
            primaryUseCase = "객체 초기화와 설정을 묶어 builder처럼 사용합니다.",
            caution = "설정 외 로직까지 섞으면 의도가 흐려집니다.",
        ),
        ScopeFunctionGuide(
            name = "also",
            primaryUseCase = "로그, 검증, 메트릭 같은 부수효과를 분리합니다.",
            caution = "핵심 비즈니스 로직을 also에 넣으면 추적이 어려워집니다.",
        ),
        ScopeFunctionGuide(
            name = "with",
            primaryUseCase = "하나의 객체 프로퍼티를 여러 번 읽을 때 문맥을 줄입니다.",
            caution = "확장 함수가 아니므로 체이닝에 남용하면 흐름이 끊깁니다.",
        ),
    )
}
