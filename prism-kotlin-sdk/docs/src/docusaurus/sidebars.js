module.exports = {
  tutorialsSidebar: [
    'tutorials',
    {
      type: 'category',
      label: 'Basic Usage Tutorial',
      items: [
        {
          type: 'autogenerated',
          dirName: 'usage-tutorial',
        }
      ]
    },
    {
      type: 'category',
      label: 'Integration Tutorial',
      items: [
        {
          type: 'autogenerated',
          dirName: 'integration-tutorial',
        }
      ]
    },
  ],
  modulesSidebar: [
    'modules',
    {
      type: 'autogenerated',
      dirName: 'modules',
    },
  ],
};
