(function () {
    const script = document.currentScript || Array.from(document.getElementsByTagName('script')).slice(-1)[0];
    if (!script) return;

    const siteKey = script.dataset.chatSiteKey || 'main-site';
    const baseUrl = (script.dataset.chatBaseUrl || new URL(script.src, window.location.href).origin).replace(/\/$/, '');
    const buttonLabel = script.dataset.chatButtonLabel || 'Чат';
    const iframeUrl = `${baseUrl}/widget/chat/frame?siteKey=${encodeURIComponent(siteKey)}&parentOrigin=${encodeURIComponent(window.location.origin)}`;

    const container = document.createElement('div');
    container.style.position = 'fixed';
    container.style.right = '24px';
    container.style.bottom = '24px';
    container.style.zIndex = '2147483000';
    container.style.display = 'flex';
    container.style.flexDirection = 'column';
    container.style.alignItems = 'flex-end';
    container.style.gap = '12px';

    const iframe = document.createElement('iframe');
    iframe.src = iframeUrl;
    iframe.title = 'FixByte chat widget';
    iframe.style.width = '380px';
    iframe.style.height = '620px';
    iframe.style.maxWidth = 'calc(100vw - 24px)';
    iframe.style.maxHeight = 'calc(100vh - 100px)';
    iframe.style.border = '0';
    iframe.style.borderRadius = '18px';
    iframe.style.boxShadow = '0 18px 48px rgba(0,0,0,.24)';
    iframe.style.overflow = 'hidden';
    iframe.style.background = '#fff';
    iframe.style.display = 'none';

    const button = document.createElement('button');
    button.type = 'button';
    button.textContent = `💬 ${buttonLabel}`;
    button.style.border = '0';
    button.style.borderRadius = '999px';
    button.style.padding = '14px 18px';
    button.style.background = 'linear-gradient(135deg, #2d3d50 0%, #3699d9 100%)';
    button.style.color = '#fff';
    button.style.font = '600 15px Segoe UI, sans-serif';
    button.style.cursor = 'pointer';
    button.style.boxShadow = '0 12px 30px rgba(54,153,217,.35)';

    let isOpen = false;

    function setOpen(next) {
        isOpen = !!next;
        iframe.style.display = isOpen ? 'block' : 'none';
        button.textContent = isOpen ? '✕ Закрыть чат' : `💬 ${buttonLabel}`;
    }

    button.addEventListener('click', () => setOpen(!isOpen));

    window.addEventListener('message', (event) => {
        if (event.origin !== baseUrl.replace(/\/$/, '')) return;
        if (event.data?.type === 'fixbyte-chat-close') {
            setOpen(false);
        }
    });

    container.appendChild(iframe);
    container.appendChild(button);
    document.body.appendChild(container);
})();

