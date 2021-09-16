import React from 'react';
import LayoutSelector from '../../Atoms/DesignTemplateStep/LayoutSelector';
import ThemeOptions from '../../Atoms/DesignTemplateStep/ThemeOptions';
import TemplateContentIcons from '../../Atoms/DesignTemplateStep/TemplateContentIcons';
import '../../_style.scss';

const StylingSettings = () => (
  <div className="ThemeBoxContainer">
    <div>
      <LayoutSelector />
    </div>
    <div className="OptionsContainer">
      <div className="themeOptions">
        <ThemeOptions />
      </div>
      <div className="templateIcons">
        <TemplateContentIcons />
      </div>
    </div>
  </div>
);

export default StylingSettings;
