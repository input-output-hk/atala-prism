import React from 'react';
import HeaderEditor from '../../Atoms/DesignTemplateStep/HeaderEditor';
import BodyEditor from '../../Atoms/DesignTemplateStep/BodyEditor';

const ContentSettings = props => (
  <div>
    <div>
      <HeaderEditor />
    </div>
    <div>
      <BodyEditor />
    </div>
  </div>
);

ContentSettings.propTypes = {};

export default ContentSettings;
