let accessToken = null;

export function getAccessToken() {
  return accessToken;
}

export function setTokens(nextAccessToken) {
  accessToken = nextAccessToken || null;
}

export function clearTokens() {
  accessToken = null;
}
