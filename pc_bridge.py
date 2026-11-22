# ULTRON Monitor PC Bridge - Windows
# Sends live PC metrics to the Android 4.2.2 app through ADB USB debugging.
import base64, json, shutil, subprocess, sys, time
try:
    import psutil
except ImportError:
    print('Instale a dependencia: py -m pip install psutil')
    input('Pressione Enter para sair...'); sys.exit(1)

ACTION = 'com.ultron.monitor.UPDATE'
last = psutil.net_io_counters()
last_time = time.time()

def get_lhm_sensors():
    # Optional: LibreHardwareMonitor running as administrator with WMI enabled.
    script = "$ErrorActionPreference='Stop'; Get-CimInstance -Namespace root/LibreHardwareMonitor -ClassName Sensor | Select-Object Name,SensorType,Value | ConvertTo-Json -Compress"
    try:
        out = subprocess.check_output(['powershell','-NoProfile','-Command',script], stderr=subprocess.DEVNULL, timeout=3, universal_newlines=True)
        data = json.loads(out)
        if isinstance(data, dict): data = [data]
        return data if isinstance(data, list) else []
    except Exception:
        return []

def find_sensor(sensors, contains, types=None):
    for s in sensors:
        name = str(s.get('Name','')).lower()
        typ = str(s.get('SensorType','')).lower()
        if contains in name and (types is None or typ in types):
            try: return round(float(s.get('Value')), 1)
            except Exception: pass
    return None

def fmt(v): return 'N/D' if v is None else v

print('ULTRON PC Bridge - conectando via ADB...')
if not shutil.which('adb'):
    print('ERRO: adb.exe nao encontrado. Instale Android Platform Tools e adicione ao PATH.')
    input('Pressione Enter para sair...'); sys.exit(1)
try:
    devices = subprocess.check_output(['adb','devices'], universal_newlines=True)
    if '\tdevice' not in devices:
        print('Tablet nao autorizado/encontrado. Ative Depuracao USB e aceite a autorizacao no tablet.')
        print(devices); input('Pressione Enter para sair...'); sys.exit(1)
except Exception as e:
    print('Falha no ADB:',e); input('Enter...'); sys.exit(1)

while True:
    try:
        now=time.time(); counters=psutil.net_io_counters(); dt=max(now-last_time,0.1)
        down=(counters.bytes_recv-last.bytes_recv)*8/dt/1000000
        up=(counters.bytes_sent-last.bytes_sent)*8/dt/1000000
        last,last_time=counters,now
        vm=psutil.virtual_memory(); disk=psutil.disk_usage('C:\\')
        freq=psutil.cpu_freq()
        sensors=get_lhm_sensors()
        cpu_temp=find_sensor(sensors,'cpu package',{'temperature'})
        if cpu_temp is None: cpu_temp=find_sensor(sensors,'core #1',{'temperature'})
        gpu_temp=find_sensor(sensors,'gpu core',{'temperature'})
        if gpu_temp is None: gpu_temp=find_sensor(sensors,'gpu',{'temperature'})
        gpu_load=find_sensor(sensors,'gpu core',{'load'})
        if gpu_load is None: gpu_load=find_sensor(sensors,'gpu',{'load'})
        vram=find_sensor(sensors,'memory used',{'small data'})
        board=find_sensor(sensors,'system',{'temperature'})
        disk_temp=find_sensor(sensors,'temperature',{'temperature'}) if any('ssd' in str(x.get('Name','')).lower() for x in sensors) else None
        fans=[]
        for s in sensors:
            if str(s.get('SensorType','')).lower()=='fan':
                try: fans.append(str(s.get('Name'))+': '+str(round(float(s.get('Value'))))+' RPM')
                except: pass
        payload={
          'cpu_percent':round(psutil.cpu_percent(interval=0.2),1), 'cpu_temp':fmt(cpu_temp),
          'cpu_freq':round(freq.current,0) if freq else 'N/D',
          'gpu_percent':fmt(gpu_load), 'gpu_temp':fmt(gpu_temp), 'vram_used':fmt(vram),
          'ram_percent':round(vm.percent,1), 'ram_used':round((vm.total-vm.available)/1073741824,1), 'ram_total':round(vm.total/1073741824,1),
          'disk_percent':round(disk.percent,1), 'disk_free':round(disk.free/1073741824,1),
          'board_temp':fmt(board), 'disk_temp':fmt(disk_temp), 'fans':', '.join(fans) if fans else 'Não disponíveis',
          'download_mbps':round(down,2), 'download_mbs':round(down/8,2), 'upload_mbps':round(up,2), 'upload_mbs':round(up/8,2)
        }
        encoded=base64.b64encode(json.dumps(payload,separators=(',',':')).encode('utf-8')).decode('ascii')
        subprocess.run(['adb','shell','am','broadcast','-a',ACTION,'--es','payload_b64',encoded], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=5)
        print('CPU {0}% | RAM {1}% | Down {2} Mbps | Up {3} Mbps'.format(payload['cpu_percent'],payload['ram_percent'],payload['download_mbps'],payload['upload_mbps']))
        time.sleep(1)
    except KeyboardInterrupt:
        print('\nEncerrado.'); break
    except Exception as e:
        print('Aviso:',e); time.sleep(2)
