export const accessFormData = {
  id: 3,
  name: 'BBMRI.cz Template',
  sections: [
    {
      id: 1,
      name: 'project',
      label: 'Project',
      description: 'Provide information about your project',
      elements: [],
      _links: {
        sections: {
          href: 'http://localhost:8081/api/v3/sections',
        },
        remove: {
          href: 'http://localhost:8081/api/v3/access-forms/3/sections/1',
        },
        add_elements: {
          href: 'http://localhost:8081/api/v3/access-forms/3/sections/1/elements',
        },
        self: {
          href: 'http://localhost:8081/api/v3/sections/3',
        },
      },
    },
    {
      id: 2,
      name: 'request',
      label: 'Request',
      description: 'Provide information the resources you are requesting',
      elements: [],
      _links: {
        sections: {
          href: 'http://localhost:8081/api/v3/sections',
        },
        remove: {
          href: 'http://localhost:8081/api/v3/access-forms/3/sections/2',
        },
        add_elements: {
          href: 'http://localhost:8081/api/v3/access-forms/3/sections/2/elements',
        },
        self: {
          href: 'http://localhost:8081/api/v3/sections/3',
        },
      },
    },
    {
      id: 3,
      name: 'ethics-vote',
      label: 'Ethics vote',
      description: 'Is ethics vote present in your project?',
      elements: [],
      _links: {
        sections: {
          href: 'http://localhost:8081/api/v3/sections',
        },
        remove: {
          href: 'http://localhost:8081/api/v3/access-forms/3/sections/3',
        },
        add_elements: {
          href: 'http://localhost:8081/api/v3/access-forms/3/sections/3/elements',
        },
        self: {
          href: 'http://localhost:8081/api/v3/sections/3',
        },
      },
    },
  ],
  _links: {
    'access-forms': {
      href: 'http://localhost:8081/api/v3/access-forms',
    },
    self: {
      href: 'http://localhost:8081/api/v3/access-forms/3',
    },
    add_sections: {
      href: 'http://localhost:8081/api/v3/access-forms/3/sections',
    },
  },
}