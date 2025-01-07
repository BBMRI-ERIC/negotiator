const devSettings = {
    auth_management_link: "http://localhost:4011"
}

const prodSettings = {
    auth_management_link: "AUTH_MANAGEMENT_LINK_PLACEHOLDER"
}

let allExternalLinks

if (import.meta.env.DEV) {
    allExternalLinks = devSettings
} else {
    allExternalLinks = prodSettings
}

export default allExternalLinks
