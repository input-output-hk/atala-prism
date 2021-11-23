import React, { useEffect } from 'react';

const Contact = () => {
  const isSsr = typeof window === 'undefined';
  const dependencies = isSsr ? [] : [window.hbspt];

  useEffect(() => {
    if (!isSsr && window.hbspt)
      window.hbspt.forms.create({
        region: 'na1',
        portalId: '8848114',
        formId: 'aa317d71-3272-44a8-94dd-e104a8e581d4',
        target: '#hubspotForm'
      });
  }, dependencies);

  return <div id="hubspotForm" />;
};

export default Contact;
