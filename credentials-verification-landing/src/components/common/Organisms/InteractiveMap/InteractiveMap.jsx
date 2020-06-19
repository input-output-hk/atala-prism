import React, { Component } from 'react';
import { AtalaPrismDemo } from 'atala-prism-demo';
import atalaLogo from '../../../../images/logo-atala-prism.svg';
import cardanoLogo from '../../../../images/logo-cardano.svg';
import './_style.scss';

const FirstStep = 1;

class InteractiveMap extends Component {
  constructor(props) {
    super(props);
    this.setMapStep = this.setMapStep.bind(this);
  }

  componentDidMount() {
    const { mapStep } = this.props;
    if (mapStep !== FirstStep) this.setMapStep();
  }

  componentDidUpdate() {
    const { mapStep, mapInitFirstStep, mapReset } = this.props;
    if (mapReset) {
      this.resetMap();
    }
    if (mapInitFirstStep || mapStep !== FirstStep) this.setMapStep();
  }

  resetMap() {
    const { onReset } = this.props;
    const { demo } = this.refs;
    try {
      if (demo) demo.reset();
    } catch {}
    onReset();
  }

  setMapStep() {
    const { mapStep } = this.props;
    const { demo } = this.refs;
    if (mapStep && demo) demo.setStep(mapStep);
  }

  render() {
    return (
      <div className="MapContainer">
        <img src={atalaLogo} alt="Atala" className="AtalaLogo" />
        <div className={'AtalaPrismDemoMap'}>
          <AtalaPrismDemo ref="demo" style={'outline:none;'} controlsEnabled={this.props.controlsEnabled}/>
        </div>
        <img src={cardanoLogo} alt="Cardano" className="CardanoLogo" />
      </div>
    );
  }
}

export default InteractiveMap;
