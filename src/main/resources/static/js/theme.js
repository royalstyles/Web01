/**
 * theme.js — 라이트/다크 모드 전환 스크립트
 *
 * ① 즉시 실행 함수(IIFE): 페이지 로드 시 HTML 파싱 직후 실행
 *    → localStorage 에 저장된 테마를 <html data-theme="..."> 에 복원
 *    → CSS 가 적용되기 전에 data-theme 을 세팅해 화면 깜빡임(FOUC) 방지
 *
 * ② toggleTheme(): 헤더 버튼 클릭 시 호출
 *    → 현재 테마를 반전해 적용 + localStorage 저장 + 버튼 아이콘 갱신
 */
(function () {
    /* 저장된 테마 복원 — 없으면 시스템 다크 모드 감지, 그것도 없으면 light */
    var saved = localStorage.getItem('theme');
    if (!saved) {
        saved = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }
    document.documentElement.setAttribute('data-theme', saved);
})();

/**
 * 테마 토글 — 헤더 버튼의 onclick 에서 호출
 */
function toggleTheme() {
    var current = document.documentElement.getAttribute('data-theme') || 'light';
    var next    = current === 'dark' ? 'light' : 'dark';

    document.documentElement.setAttribute('data-theme', next);
    localStorage.setItem('theme', next);

    /* 버튼 아이콘 갱신 */
    var btn = document.getElementById('theme-toggle-btn');
    if (btn) btn.textContent = next === 'dark' ? '☀️' : '🌙';
}

/* DOM 로드 완료 후 버튼 아이콘 초기화 */
document.addEventListener('DOMContentLoaded', function () {
    var theme = document.documentElement.getAttribute('data-theme') || 'light';
    var btn   = document.getElementById('theme-toggle-btn');
    if (btn) btn.textContent = theme === 'dark' ? '☀️' : '🌙';
});
