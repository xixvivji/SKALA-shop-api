(() => {
  const localHosts = new Set(["localhost", "127.0.0.1", "::1", "[::1]"]);
  const isLocalDevelopment = localHosts.has(window.location.hostname);
  const configuredUrl = window.SKALA_CONFIG?.API_BASE_URL?.trim();
  const storedUrl = isLocalDevelopment
    ? window.localStorage.getItem("skala-api-base-url")?.trim()
    : "";
  const defaultUrl = isLocalDevelopment
    ? `${window.location.protocol}//${window.location.hostname}:8080`
    : window.location.origin;
  const candidateUrl = configuredUrl || storedUrl || defaultUrl;
  let apiBaseUrl = defaultUrl;

  try {
    const parsedUrl = new URL(candidateUrl, window.location.origin);
    const supportedProtocol = ["http:", "https:"].includes(parsedUrl.protocol);
    const mixedContent =
      window.location.protocol === "https:" && parsedUrl.protocol !== "https:";
    if (supportedProtocol && !mixedContent) apiBaseUrl = parsedUrl.href;
  } catch {
    // Keep the environment-aware default when deployment configuration is invalid.
  }

  window.SKALA_CONFIG = {
    ...window.SKALA_CONFIG,
    API_BASE_URL: apiBaseUrl,
  };
})();
