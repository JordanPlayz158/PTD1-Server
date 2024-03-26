<?php

if (STORAGE_METHOD === 'MYSQL')
{
    require_once '../../MySQL.php';
}
else
{
    require_once 'json.php';
}

const letterList = ['m', 'y', 'w', 'c', 'q', 'a', 'p', 'r', 'e', 'o'];

function convertToString(int $param1)//: string
{
    if ($param1 < count(letterList))
    {
        return letterList[$param1];
    }
    return '-1';
}

function convertToInt(string $param1)//: string
{
    for ($i = 0; $i < count(letterList); $i++)
    {
        if (strcmp($param1,letterList[$i]) === 0)
        {
            return "$i";
        }
    }
    return '-1';
}

function convertStringToIntString(string $param1)//: string
{
    $loc3 = '';
    for ($i = 0; $i < strlen($param1); $i++)
    {
        $loc4 = convertToInt($param1[$i]);
        if ($loc4 === '-1')
        {
            return '-100';
        }
        $loc3 .= $loc4;
    }
    return $loc3;
}

function convertStringToInt(string $param1)//: int
{
    return (int) convertStringToIntString($param1);
}

function convertIntToString(int $num)
{
    $result = '';
    $str_int = "$num";
    for ($i = 0; $i < strlen($str_int); $i++)
    {
        $loc5 = convertToString((int) $str_int[$i]);
        if ($loc5 === '-1')
        {
            return '-100';
        }
        $result .= $loc5;
    }
    return $result;
}

function get_Length(int $param1, int $param2)//: string
{
    $loc3 = $param1 + $param2 + 1;
    $loc5 = "$loc3";
    $loc4 = strlen($loc5);

    if ($loc4 != $param1)
    {
        return get_Length($loc4, $param2);
    }
    return "$loc4$loc5";
}

function create_Check_Sum(string $encoded_info)
{
    $checksum = 15;
    for ($i = 0; $i < strlen($encoded_info); $i++)
    {
        $char = $encoded_info[$i];
        if (is_numeric($char))
        {
            $checksum += (int) $encoded_info[$i];
        }
        else
        {
            $checksum += ord($encoded_info[$i]) - 96;
        }
    }
    return $checksum * 3;
}

function decode_pokeinfo(string $encoded_pokeinfo, $email)//: array
{
    $pokemons = array();
    $AvaliableSaveID = get_avaliable_saveID($email);
    $pointer = 0;
    $data_len_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
    $pointer += $data_len_len;

    $pokes_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
    $pokes = convertStringToInt(substr($encoded_pokeinfo, $pointer, $pokes_len));
    $pointer += $pokes_len;
    for ($i = 0; $i < $pokes; $i++)
    {
        $poke = array();
        $poke['reason'] = array();
        $poke_info_len_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
        $poke_info_len =  convertStringToInt(substr($encoded_pokeinfo, $pointer, $poke_info_len_len));
        $pointer += $poke_info_len_len;

        $saveID_len_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
        $saveID_len = convertStringToInt(substr($encoded_pokeinfo, $pointer, $saveID_len_len));
        $pointer += $saveID_len_len;
        $saveID = convertStringToInt(substr($encoded_pokeinfo, $pointer, $saveID_len));
        $pointer += $saveID_len;
        $poke['saveID'] = $saveID;
        for ($j = 0; $j < $poke_info_len; $j++)
        {
            $info_type_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
            $info_type = convertStringToInt(substr($encoded_pokeinfo, $pointer, $info_type_len));
            $pointer += $info_type_len;
            switch ($info_type)
            {
            case 1: // Captured
                $poke['needNickname'] = $i + 1;
                $poke['saveID'] = $AvaliableSaveID++;

                $num_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $num = convertStringToInt(substr($encoded_pokeinfo, $pointer, $num_len));
                $poke['num'] = $num;
                $pointer += $num_len;

                $xp_len_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $xp_len = convertStringToInt(substr($encoded_pokeinfo, $pointer, $xp_len_len));
                $pointer += $xp_len_len;
                $xp = convertStringToInt(substr($encoded_pokeinfo, $pointer, $xp_len));
                $poke['xp'] = $xp;
                $pointer += $xp_len;

                $lvl_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $lvl = convertStringToInt(substr($encoded_pokeinfo, $pointer, $lvl_len));
                $poke['lvl'] = $lvl;
                $pointer += $lvl_len;

                $move1_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $move1 = convertStringToInt(substr($encoded_pokeinfo, $pointer, $move1_len));
                $poke['move1'] = $move1;
                $pointer += $move1_len;

                $move2_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $move2 = convertStringToInt(substr($encoded_pokeinfo, $pointer, $move2_len));
                $poke['move2'] = $move2;
                $pointer += $move2_len;

                $move3_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $move3 = convertStringToInt(substr($encoded_pokeinfo, $pointer, $move3_len));
                $poke['move3'] = $move3;
                $pointer += $move3_len;

                $move4_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $move4 = convertStringToInt(substr($encoded_pokeinfo, $pointer, $move4_len));
                $poke['move4'] = $move4;
                $pointer += $move4_len;

                $tt_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $tt = convertStringToInt(substr($encoded_pokeinfo, $pointer, $tt_len)); // Targeting Type
                $poke['targetingType'] = $tt;
                $pointer += $tt_len;

                $gender_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $gender = convertStringToInt(substr($encoded_pokeinfo, $pointer, $gender_len));
                $poke['gender'] = $gender;
                $pointer += $gender_len;

                $pos_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $pos = convertStringToInt(substr($encoded_pokeinfo, $pointer, $pos_len));
                $poke['pos'] = $pos;
                $pointer += $pos_len;

                $extra_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $extra = convertStringToInt(substr($encoded_pokeinfo, $pointer, $extra_len));
                if ($extra != 0)
                {
                    $extra = $extra == $num ? 1 : 2;
                }
                $poke['extra'] = $extra;
                $pointer += $extra_len;

                $item_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $item = convertStringToInt(substr($encoded_pokeinfo, $pointer, $item_len));
                $poke['item'] = $item;
                $pointer += $item_len;

                $tag_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $tag = substr($encoded_pokeinfo, $pointer, $tag_len);
                $poke['tag'] = $tag;
                $pointer += $tag_len;

                $poke['needSaveID'] = $poke['pos'];
                break;
            case 2: // Level up
                $lvl_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $lvl = convertStringToInt(substr($encoded_pokeinfo, $pointer, $lvl_len));
                $poke['lvl'] = $lvl;
                $pointer += $lvl_len;
                break;
            case 3: // XP up
                $xp_len_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $xp_len = convertStringToInt(substr($encoded_pokeinfo, $pointer, $xp_len_len));
                $pointer += $xp_len_len;
                $xp = convertStringToInt(substr($encoded_pokeinfo, $pointer, $xp_len));
                $poke['xp'] = $xp;
                $pointer += $xp_len;
                break;
            case 4: // Change moves
                $move1_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $move1 = convertStringToInt(substr($encoded_pokeinfo, $pointer, $move1_len));
                $poke['move1'] = $move1;
                $pointer += $move1_len;

                $move2_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $move2 = convertStringToInt(substr($encoded_pokeinfo, $pointer, $move2_len));
                $poke['move2'] = $move2;
                $pointer += $move2_len;

                $move3_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $move3 = convertStringToInt(substr($encoded_pokeinfo, $pointer, $move3_len));
                $poke['move3'] = $move3;
                $pointer += $move3_len;

                $move4_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $move4 = convertStringToInt(substr($encoded_pokeinfo, $pointer, $move4_len));
                $poke['move4'] = $move4;
                $pointer += $move4_len;
                break;
            case 5: // Change Item
                $item_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $item = convertStringToInt(substr($encoded_pokeinfo, $pointer, $item_len));
                $poke['item'] = $item;
                $pointer += $item_len;
                break;
            case 6: // Evolve
                $num_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $num = convertStringToInt(substr($encoded_pokeinfo, $pointer, $num_len));
                $poke['num'] = $num;
                $pointer += $num_len;
                break;
            case 7: // Change Nickname
                $poke['needNickname'] = $i + 1;
                break;
            case 8: // Pos change
                $pos_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $pos = convertStringToInt(substr($encoded_pokeinfo, $pointer, $pos_len));
                $poke['pos'] = $pos;
                $pointer += $pos_len;
                break;
            case 9: // Need Tag
                $tag_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $tag = substr($encoded_pokeinfo, $pointer, $tag_len);
                $poke['tag'] = $tag;
                $pointer += $tag_len;
                break;
            case 10: // Need Trade
                $num_len = convertStringToInt($encoded_pokeinfo[$pointer++]);
                $num = convertStringToInt(substr($encoded_pokeinfo, $pointer, $num_len));
                $poke['num'] = $num;
                $pointer += $num_len;
                break;
            }
            array_push($poke['reason'], $info_type);
        }
        $pokemons[$poke['saveID']] = $poke;
    }
    return $pokemons;
}

function encode_pokemons($pokemons)
{
    $NPokes = convertIntToString(count($pokemons));
    $NPokes_len = convertIntToString(strlen($NPokes));
    $encoded_pokes = "$NPokes_len$NPokes";
    $pokenicks = '';
    $parts = [];
    foreach ($pokemons as $poke)
    {
        // this is to avoid suspect of hacking
        // For some reason sometimes the game
        // sends only the new pos of one poke
        if (isset($parts[$poke['pos']]))
        {
            for ($i=0; $i<count($pokemons); $i++)
            {
                if (!isset($parts[$i]))
                {
                    $poke['pos'] = $i;
                    break;
                }
            }
        }
        $num = convertIntToString($poke['num']);
        $num_len = convertIntToString(strlen($num));
        $xp = convertIntToString($poke['xp']);
        $xp_len = convertIntToString(strlen($xp));
        $xp_len_len = convertIntToString(strlen($xp_len));
        $lvl = convertIntToString($poke['lvl']);
        $lvl_len = convertIntToString(strlen($lvl));
        $move1 = convertIntToString($poke['move1']);
        $move1_len = convertIntToString(strlen($move1));
        $move2 = convertIntToString($poke['move2']);
        $move2_len = convertIntToString(strlen($move2));
        $move3 = convertIntToString($poke['move3']);
        $move3_len = convertIntToString(strlen($move3));
        $move4 = convertIntToString($poke['move4']);
        $move4_len = convertIntToString(strlen($move4));
        $tt = convertIntToString($poke['targetingType']);
        $tt_len = convertIntToString(strlen($tt));
        $gender = convertIntToString($poke['gender']);
        $gender_len = convertIntToString(strlen($gender));
        $saveID = convertIntToString($poke['saveID']);
        $saveID_len = convertIntToString(strlen($saveID));
        $saveID_len_len = convertIntToString(strlen($saveID_len));
        $pos = convertIntToString($poke['pos']);
        $pos_len = convertIntToString(strlen($pos));
        $extra = convertIntToString($poke['extra']);
        $extra_len = convertIntToString(strlen($extra));
        $item = convertIntToString($poke['item']);
        $item_len = convertIntToString(strlen($item));
        $tag = $poke['tag'];
        $tag_len = convertIntToString(strlen($tag));

        $tmp = $poke['pos'] + 1;
        $pokenicks .= "&PN{$tmp}={$poke['Nickname']}";

        $parts[$poke['pos']] = $num_len . $num . $xp_len_len . $xp_len . $xp
           . $lvl_len . $lvl . $move1_len . $move1 . $move2_len . $move2
           . $move3_len . $move3 . $move4_len . $move4 . $tt_len . $tt
           . $gender_len . $gender . $saveID_len_len . $saveID_len . $saveID . $pos_len . $pos
           . $extra_len . $extra . $item_len . $item . $tag_len . $tag;
        }
    ksort($parts);
    $encoded_pokes .= join('', $parts);
    $encoded_len = strlen($encoded_pokes);
    $encoded_len_len = strlen($encoded_len);
    $final_len = convertIntToString(get_Length($encoded_len_len, $encoded_len));
    $encoded_pokes = $final_len . $encoded_pokes;
    return [$encoded_pokes, $pokenicks];
}

function encode_inventory($items)//: string
{
    $encoded_items = '';
    $qnt = 0;
    foreach ($items as $num => $quantity)
    {
        if ($quantity > 0)
        {
            $qnt++;
            $encoded_num = convertIntToString($num);
            $num_len = convertIntToString(strlen($encoded_num));
            $quantity = convertIntToString($quantity);
            $quantity_len = convertIntToString(strlen($quantity));

            $encoded_items .= $num_len . $encoded_num . $quantity_len . $quantity;
        }
    }
    $inventory_len = convertIntToString($qnt);
    $inventory_len_len = convertIntToString(strlen($inventory_len));

    $encoded_items = $inventory_len_len . $inventory_len . $encoded_items;
    $encoded_len = strlen($encoded_items);
    $encoded_len_len = strlen($encoded_len);
    $final_len = convertIntToString(get_Length($encoded_len_len, $encoded_len));

    $encoded_items = $final_len . $encoded_items;

    return $encoded_items;
}

function decode_inventory(string $encoded_items)//: array
{
    $items = array();
    $pointer = 0;
    $encoded_len_len = convertStringToInt($encoded_items[$pointer++]);
    $pointer += $encoded_len_len;
    $inventory_len_len = convertStringToInt($encoded_items[$pointer++]);
    $inventory_len = convertStringToInt(substr($encoded_items, $pointer, $inventory_len_len));
    $pointer += $inventory_len_len;

    for ($i = 0; $i < $inventory_len; $i++)
    {
        $num_len = convertStringToInt($encoded_items[$pointer++]);
        $num = convertStringToInt(substr($encoded_items, $pointer, $num_len));
        $pointer += $num_len;
        $quantity_len = convertStringToInt($encoded_items[$pointer++]);
        $quantity = convertStringToInt(substr($encoded_items, $pointer, $quantity_len));
        $pointer += $quantity_len;
        $items[$num] = $quantity;
    }
    return $items;
}

function decode_extra(string $encoded_extra)//: array
{
    $extra = decode_inventory($encoded_extra);
    return $extra;
}

function decode_1v1(string $encoded_data)//: array
{
    $data = array();

    // Getting the size of data
    $tmp1 = 0;
    $len1 = convertStringToInt($encoded_data[$tmp1++]);
    $len2 = convertStringToInt(substr($encoded_data, $tmp1, $len1));
    $tmp1 += $len1;

    $money_len = convertStringToInt($encoded_data[$tmp1++]);
    $money = convertStringToInt(substr($encoded_data, $tmp1, $money_len));
    $tmp1 += $money_len;
    $levels_lenght = convertStringToInt($encoded_data[$tmp1++]);
    $levels_unlocked = convertStringToInt(substr($encoded_data, $tmp1, $levels_lenght));

    $data['money'] = $money;
    $data['levelUnlocked'] = $levels_unlocked;
    return $data;
}

function encode_1v1(array $profiles)//: string
{
    $encoded_data = '';
    $PA = 0;

    foreach ($profiles as $key => $profile)
    {
        $PA++;
        $whichProfile = convertIntToString($key[7]);
        $encoded_money = convertIntToString($profile['money']);
        $money_len = convertIntToString(strlen($encoded_money));

        $encoded_levels = convertIntToString($profile['levelUnlocked']);
        $levels_len = convertIntToString(strlen($encoded_levels));

        $encoded_data .= $whichProfile . $money_len . $encoded_money . $levels_len . $encoded_levels;
    }
    $PA = convertIntToString($PA);
    $encoded_data = $PA . $encoded_data;
    $data_len = strlen($encoded_data);
    $data_len_len = strlen($data_len);
    $encoded_len = convertIntToString(get_Length($data_len_len, $data_len));

    $encoded_data = $encoded_len .  $encoded_data;

    return $encoded_data;
}

function encode_story(array $story_data)//: string
{
    $encoded_data = '';
    $PA = 0;

    for ($i = 1; $i <= 3; $i++)
    {
        if (array_key_exists("profile$i", $story_data))
        {
            $profile = $story_data["profile$i"];
            $PA++;
            $whichProfile = convertIntToString($i);
            $encoded_money = convertIntToString($profile['Money']);
            $money_len = convertIntToString(strlen($encoded_money));

            $encoded_badges = convertIntToString(get_badges($profile['extra']));
            $badges_len = convertIntToString(strlen($encoded_badges));

            $encoded_data .= $whichProfile . $money_len . $encoded_money . $badges_len . $encoded_badges;
        }
    }
    $PA = convertIntToString($PA);
    $encoded_data .= $PA;
    $data_len = strlen($encoded_data);
    $data_len_len = strlen($data_len);
    $encoded_len = convertIntToString(get_Length($data_len_len, $data_len));

    $encoded_data = $encoded_len . $PA . $encoded_data;

    return $encoded_data;
}

function encode_story_profile(array $profile)//: array
{
    $encoded_data = array();
    $extra = '';

    $currentMap = convertIntToString($profile['MapLoc']);
    $Map_len = convertIntToString(strlen($currentMap));
    $currentSpot = convertIntToString($profile['MapSpot']);
    $Spot_len = convertIntToString(strlen($currentSpot));
    $extra = $Map_len . $currentMap . $Spot_len . $currentSpot;

    $extra_len = strlen($extra);
    $extra_len_len = strlen($extra_len);
    $encoded_extra_len = convertIntToString(get_Length($extra_len_len, $extra_len));

    $extra = $encoded_extra_len . $extra;

    $extra2 = encode_inventory($profile['extra']); // Extra info is compatible with items encoding method

    $extra3 = encode_pokemons($profile['poke']);

    $extra4 = encode_inventory($profile['items']);

    $extra5 = convertIntToString(create_Check_Sum($extra3[0] . $profile['CurrentSave']));
    $encoded_data = [$extra, $extra2, $extra3, $extra4, $extra5];
    return $encoded_data;
}

function get_badges(array $extra)//: int
{
    $badges = 0;
    if (isset($extra[48]) && $extra[48] === 2)
    {
        $badges = 1;
    }
    if (isset($extra[59]) && $extra[59] === 2)
    {
        $badges = 2;
    }
    if (isset($extra[64]))
    {
        if ($extra[64] >= 12)
        {
            $badges = 8;
        }
        else if ($extra[64] >= 11)
        {
            $badges = 7;
        }
        else if ($extra[64] >= 9)
        {
            $badges = 5;
        }
        else if ($extra[64] >= 7)
        {
            $badges = 4;
        }
        else if ($extra[64] >= 1)
        {
            $badges = 3;
        }
    }
    return $badges;
}
?>
