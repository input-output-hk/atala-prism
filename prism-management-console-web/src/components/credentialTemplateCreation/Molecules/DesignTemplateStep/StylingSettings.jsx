import React from 'react';
import LayoutSelector from '../../Atoms/DesignTemplateStep/LayoutSelector';
import ThemeOptions from '../../Atoms/DesignTemplateStep/ThemeOptions';
import TemplateIcons from '../../Atoms/DesignTemplateStep/TemplateIcons';
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
        <TemplateIcons />
      </div>
    </div>
  </div>
);

export default StylingSettings;
